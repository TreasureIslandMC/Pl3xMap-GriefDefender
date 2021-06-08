package net.pl3x.map.griefdefender.hook;

import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;

import java.util.Collection;
import java.util.UUID;

public class GPHook {
    public static boolean isWorldEnabled(UUID uuid) {
        return GriefDefender.getCore().isEnabled(uuid);
    }

    public static Collection<Claim> getClaims() {
        return GriefDefender.getCore().getAllClaims();
    }
}
