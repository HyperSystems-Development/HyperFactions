package com.hyperfactions.protection.ecs;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.protection.ProtectionListener;
import org.jetbrains.annotations.NotNull;

/**
 * Alias for DamageProtectionSystem for backward compatibility.
 * All damage protection (including PvP) is handled by DamageProtectionSystem.
 */
public class PvPProtectionSystem extends DamageProtectionSystem {

  /** Creates a new PvPProtectionSystem. */
  public PvPProtectionSystem(@NotNull HyperFactions hyperFactions,
                @NotNull ProtectionListener protectionListener) {
    super(hyperFactions, protectionListener);
  }
}
