package net.pl3x.map.griefdefender.task;

import com.flowpowered.math.vector.Vector3i;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.TrustTypes;
import net.pl3x.map.api.Key;
import net.pl3x.map.api.MapWorld;
import net.pl3x.map.api.Point;
import net.pl3x.map.api.SimpleLayerProvider;
import net.pl3x.map.api.marker.Marker;
import net.pl3x.map.api.marker.MarkerOptions;
import net.pl3x.map.api.marker.Rectangle;
import net.pl3x.map.griefdefender.configuration.Config;
import net.pl3x.map.griefdefender.hook.GPHook;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class Pl3xMapTask extends BukkitRunnable {
    private final MapWorld world;
    private final SimpleLayerProvider provider;

    private boolean stop;

    public Pl3xMapTask(MapWorld world, SimpleLayerProvider provider) {
        this.world = world;
        this.provider = provider;
    }

    @Override
    public void run() {
        if (stop) {
            cancel();
        }
        updateClaims();
    }

    void updateClaims() {
        provider.clearMarkers(); // TODO track markers instead of clearing them
        Collection<Claim> topLevelClaims = GPHook.getClaims();
        if (topLevelClaims != null) {
            topLevelClaims.stream()
                    .filter(claim -> claim.getWorldUniqueId().equals(this.world.uuid()))
                    .filter(claim -> !claim.getParent().isPresent())
                    .forEach(this::handleClaim);
        }
    }

    private void handleClaim(Claim claim) {
        Vector3i min = claim.getLesserBoundaryCorner();
        Vector3i max = claim.getGreaterBoundaryCorner();
        if (min == null) {
            return;
        }
        Rectangle rect = Marker.rectangle(Point.of(min.getX(), min.getZ()), Point.of(max.getX() + 1, max.getZ() + 1));

        ArrayList<UUID> builders = new ArrayList<>(claim.getUserTrusts(TrustTypes.BUILDER));
        ArrayList<UUID> containers = new ArrayList<>(claim.getUserTrusts(TrustTypes.CONTAINER));
        ArrayList<UUID> accessors = new ArrayList<>(claim.getUserTrusts(TrustTypes.ACCESSOR));
        ArrayList<UUID> managers = new ArrayList<>(claim.getUserTrusts(TrustTypes.MANAGER));

        String worldName = Bukkit.getWorld(claim.getWorldUniqueId()).getName();

        MarkerOptions.Builder options = MarkerOptions.builder()
                .strokeColor(Config.STROKE_COLOR)
                .strokeWeight(Config.STROKE_WEIGHT)
                .strokeOpacity(Config.STROKE_OPACITY)
                .fillColor(Config.FILL_COLOR)
                .fillOpacity(Config.FILL_OPACITY)
                .clickTooltip((claim.isAdminClaim() ? Config.ADMIN_CLAIM_TOOLTIP : Config.CLAIM_TOOLTIP)
                        .replace("{world}", worldName)
                        .replace("{id}", claim.getUniqueId().toString())
                        .replace("{owner}", claim.getOwnerName())
                        .replace("{managers}", getNames(managers))
                        .replace("{builders}", getNames(builders))
                        .replace("{containers}", getNames(containers))
                        .replace("{accessors}", getNames(accessors))
                        .replace("{area}", Integer.toString(claim.getArea()))
                        .replace("{width}", Integer.toString(claim.getWidth()))
                        .replace("{height}", Integer.toString(claim.getHeight()))
                );

        if (claim.isAdminClaim()) {
            options.strokeColor(Color.BLUE).fillColor(Color.BLUE);
        }

        rect.markerOptions(options);

        String markerid = "griefprevention_" + worldName + "_region_" + claim.getUniqueId().toString();
        this.provider.addMarker(Key.of(markerid), rect);
    }

    private static String getNames(List<UUID> list) {
        List<String> names = new ArrayList<>();
        for (final UUID uuid : list) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            names.add(offlinePlayer.getName());
        }
        return String.join(", ", names);
    }

    public void disable() {
        cancel();
        this.stop = true;
        this.provider.clearMarkers();
    }
}

