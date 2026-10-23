package com.minicloud.infrastructure.ceph;

import com.minicloud.common.exception.CephAlreadyExistsException;
import com.minicloud.common.exception.CephInUseException;
import com.minicloud.common.exception.CephNotAvailableException;
import com.minicloud.common.exception.CephNotFoundException;
import com.minicloud.common.exception.CephOperationException;
import com.minicloud.common.exception.InvalidCephConfigurationException;
import com.minicloud.infrastructure.process.CommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Talks to Ceph via {@code ceph} / {@code rbd}. Ceph is the source of truth.
 */
@Component
public class MicroCephProvider implements CephProvider {

    private static final Logger log = LoggerFactory.getLogger(MicroCephProvider.class);

    private final CephCli cephCli;

    public MicroCephProvider(CephCli cephCli) {
        this.cephCli = cephCli;
    }

    @Override
    public CephCapabilities capabilities() {
        CommandResult version = cephCli.version();
        if (!version.succeeded()) {
            String stderr = firstNonBlank(version.stderr(), version.stdout());
            if (looksMissing(stderr)) {
                return new CephCapabilities(false, false, "NOT_INSTALLED",
                        "ceph CLI is not installed. Install MicroCeph (snap install microceph) for Phase 8.",
                        null, null);
            }
            return new CephCapabilities(true, false, "UNAVAILABLE",
                    "ceph --version failed: " + stderr, null, null);
        }
        String cephVersion = CephOutputParser.parseVersion(version.stdout());
        CommandResult status = cephCli.status();
        if (!status.succeeded()) {
            String detail = firstNonBlank(status.stderr(), status.stdout());
            if (looksPermission(detail) || looksAuth(detail)) {
                return new CephCapabilities(true, false, "PERMISSION_DENIED",
                        "Ceph is installed but the client cannot authenticate: " + detail,
                        cephVersion, null);
            }
            return new CephCapabilities(true, false, "UNAVAILABLE", detail, cephVersion, null);
        }
        String fsid = null;
        CommandResult fsidResult = cephCli.fsid();
        if (fsidResult.succeeded()) {
            fsid = fsidResult.stdout() == null ? null : fsidResult.stdout().trim();
        }
        return new CephCapabilities(true, true, "CONNECTED",
                "Connected to Ceph via ceph/rbd CLI.", cephVersion, fsid);
    }

    @Override
    public CephClusterStatus status() {
        requireConnected();
        CommandResult result = cephCli.status();
        if (!result.succeeded()) {
            throw new CephOperationException("ceph -s failed: " + firstNonBlank(result.stderr(), result.stdout()));
        }
        return CephOutputParser.parseStatusText(result.stdout());
    }

    @Override
    public List<CephPoolInfo> listPools() {
        CephCapabilities caps = capabilities();
        if (!caps.connected()) {
            log.info("Skipping Ceph pool listing: status={}", caps.status());
            return List.of();
        }
        CommandResult detail = cephCli.poolLsDetail();
        if (!detail.succeeded()) {
            log.warn("ceph osd pool ls detail failed: {}", firstNonBlank(detail.stderr(), detail.stdout()));
            return List.of();
        }
        List<CephPoolInfo> pools = CephOutputParser.parsePoolDetailsJson(detail.stdout());
        CommandResult df = cephCli.df();
        if (df.succeeded()) {
            CephOutputParser.applyDfStats(pools, df.stdout());
        }
        log.info("Ceph reported {} pool(s)", pools.size());
        return pools;
    }

    @Override
    public CephPoolInfo getPool(String name) {
        requireConnected();
        CephNames.requireSafe(name, "pool");
        CephPoolInfo pool = findPool(name);
        if (pool == null) {
            throw new CephNotFoundException("pool", name);
        }
        List<RbdImageInfo> images = listImages(name);
        return pool.withImages(images);
    }

    @Override
    public CephPoolInfo createPool(CreateCephPoolRequest request) {
        requireConnected();
        String name = CephNames.requireManagedPool(request.name());
        if (findPool(name) != null) {
            throw new CephAlreadyExistsException("pool", name);
        }
        int pgNum = request.pgNum() > 0 ? request.pgNum() : 8;
        log.info("Creating Ceph pool {} pg_num={}", name, pgNum);
        CommandResult create = cephCli.poolCreate(name, pgNum);
        if (!create.succeeded()) {
            throw new CephOperationException("ceph osd pool create failed: "
                    + firstNonBlank(create.stderr(), create.stdout()));
        }
        // Single-node MicroCeph labs often have fewer than 3 OSDs.
        cephCli.poolSet(name, "size", "2");
        cephCli.poolSet(name, "min_size", "1");
        CommandResult app = cephCli.poolApplicationEnable(name, "rbd");
        if (!app.succeeded()) {
            log.warn("Could not enable rbd application on {}: {}", name, firstNonBlank(app.stderr(), app.stdout()));
        }
        CommandResult init = cephCli.rbdPoolInit(name);
        if (!init.succeeded()) {
            log.warn("rbd pool init {}: {}", name, firstNonBlank(init.stderr(), init.stdout()));
        }
        CephPoolInfo created = findPool(name);
        if (created == null) {
            throw new CephOperationException("ceph reported success but pool '" + name + "' is missing.");
        }
        return created;
    }

    @Override
    public void deletePool(String name) {
        requireConnected();
        CephNames.requireSafe(name, "pool");
        CephPoolInfo pool = findPool(name);
        if (pool == null) {
            throw new CephNotFoundException("pool", name);
        }
        if (!pool.managedByMiniCloud()) {
            throw new InvalidCephConfigurationException(
                    "Refusing to delete pool '" + name + "'. MiniCloud only deletes pools named mc-*.");
        }
        List<RbdImageInfo> images = listImages(name);
        if (!images.isEmpty()) {
            throw new CephInUseException(
                    "Pool '" + name + "' still has RBD images "
                            + images.stream().map(RbdImageInfo::name).toList()
                            + ". Delete images first.");
        }
        log.info("Deleting Ceph pool {}", name);
        CommandResult result = cephCli.poolDelete(name);
        if (!result.succeeded()) {
            throw new CephOperationException("ceph osd pool delete failed: "
                    + firstNonBlank(result.stderr(), result.stdout()));
        }
    }

    @Override
    public List<RbdImageInfo> listImages(String pool) {
        requireConnected();
        CephNames.requireSafe(pool, "pool");
        if (findPool(pool) == null) {
            throw new CephNotFoundException("pool", pool);
        }
        CommandResult result = cephCli.rbdLs(pool);
        if (!result.succeeded()) {
            String detail = firstNonBlank(result.stderr(), result.stdout());
            if (detail.toLowerCase(Locale.ROOT).contains("no such file")
                    || detail.toLowerCase(Locale.ROOT).contains("doesn't exist")) {
                return List.of();
            }
            throw new CephOperationException("rbd ls failed: " + detail);
        }
        List<RbdImageInfo> images = new ArrayList<>();
        for (String imageName : CephOutputParser.parseRbdNamesJson(result.stdout())) {
            images.add(describeImage(pool, imageName));
        }
        return images;
    }

    @Override
    public RbdImageInfo getImage(String pool, String image) {
        requireConnected();
        CephNames.requireSafe(pool, "pool");
        CephNames.requireImage(image);
        RbdImageInfo info = describeImage(pool, image);
        if (info == null) {
            throw new CephNotFoundException("image", pool + "/" + image);
        }
        return info;
    }

    @Override
    public RbdImageInfo createImage(CreateRbdImageRequest request) {
        requireConnected();
        String pool = CephNames.requireSafe(request.pool(), "pool");
        String image = CephNames.requireImage(request.name());
        if (findPool(pool) == null) {
            throw new CephNotFoundException("pool", pool);
        }
        if (imageExists(pool, image)) {
            throw new CephAlreadyExistsException("image", pool + "/" + image);
        }
        long sizeGb = request.sizeGb() > 0 ? request.sizeGb() : 1;
        log.info("Creating RBD image {}/{} size={}G", pool, image, sizeGb);
        CommandResult result = cephCli.rbdCreate(pool, image, sizeGb);
        if (!result.succeeded()) {
            throw new CephOperationException("rbd create failed: " + firstNonBlank(result.stderr(), result.stdout()));
        }
        RbdImageInfo created = describeImage(pool, image);
        if (created == null) {
            throw new CephOperationException("rbd create succeeded but image is missing.");
        }
        return created;
    }

    @Override
    public void deleteImage(String pool, String image) {
        requireConnected();
        CephNames.requireSafe(pool, "pool");
        CephNames.requireImage(image);
        if (!imageExists(pool, image)) {
            throw new CephNotFoundException("image", pool + "/" + image);
        }
        log.info("Deleting RBD image {}/{}", pool, image);
        CommandResult result = cephCli.rbdRemove(pool, image);
        if (!result.succeeded()) {
            throw new CephOperationException("rbd rm failed: " + firstNonBlank(result.stderr(), result.stdout()));
        }
    }

    private RbdImageInfo describeImage(String pool, String image) {
        CommandResult info = cephCli.rbdInfo(pool, image);
        if (!info.succeeded()) {
            return null;
        }
        RbdImageInfo parsed = CephOutputParser.parseRbdInfoJson(pool, info.stdout());
        if (parsed == null) {
            return null;
        }
        long used = 0;
        CommandResult du = cephCli.rbdDu(pool, image);
        if (du.succeeded()) {
            used = CephOutputParser.parseRbdUsedBytes(du.stdout());
        }
        return new RbdImageInfo(
                parsed.name(), parsed.pool(), parsed.sizeBytes(), used, parsed.format(),
                List.of(), parsed.detail());
    }

    private CephPoolInfo findPool(String name) {
        return listPools().stream().filter(pool -> pool.name().equals(name)).findFirst().orElse(null);
    }

    private boolean imageExists(String pool, String image) {
        CommandResult info = cephCli.rbdInfo(pool, image);
        return info.succeeded();
    }

    private void requireConnected() {
        CephCapabilities caps = capabilities();
        if (!caps.connected()) {
            throw new CephNotAvailableException(caps.detail());
        }
    }

    private static boolean looksMissing(String message) {
        if (message == null) {
            return true;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("no such file") || lower.contains("not found") || lower.contains("cannot run program");
    }

    private static boolean looksPermission(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("permission") || lower.contains("denied");
    }

    private static boolean looksAuth(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("keyring") || lower.contains("authenticate") || lower.contains("errno 95")
                || lower.contains("error connecting");
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "unknown error";
    }
}
