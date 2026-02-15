package com.hyperfactions.gui.admin.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.jetbrains.annotations.Nullable;

/**
 * Event data for the Admin Zone management page.
 */
public class AdminZoneData implements AdminNavAwareData {

    /** The button/action that triggered the event */
    public String button;

    /** Zone ID (if any) */
    public String zoneId;

    /** Zone UUID for expansion toggle */
    public String zoneUuid;

    /** Zone name (if any) */
    public String zoneName;

    /** Zone type (safe, war) */
    public String zoneType;

    /** Target chunk X coordinate */
    public int chunkX;

    /** Target chunk Z coordinate */
    public int chunkZ;

    /** Current page number (for pagination) */
    public int page;

    /** Zone name input from wizard */
    public String inputName;

    /** Claiming method selection (no_claims, single_chunk, radius_circle, radius_square, use_map) */
    public String claimMethod;

    /** Selected radius preset value */
    public String radius;

    /** Custom radius input value */
    public String customRadius;

    /** Flags choice selection (defaults, customize) */
    public String flagsChoice;

    /** Sort mode from dropdown (dynamic) */
    public String sortMode;

    /** Admin nav bar target (for navigation) */
    public String adminNavBar;

    /** Codec for serialization/deserialization */
    public static final BuilderCodec<AdminZoneData> CODEC = BuilderCodec
            .builder(AdminZoneData.class, AdminZoneData::new)
            .addField(
                    new KeyedCodec<>("Button", Codec.STRING),
                    (data, value) -> data.button = value,
                    data -> data.button
            )
            .addField(
                    new KeyedCodec<>("ZoneId", Codec.STRING),
                    (data, value) -> data.zoneId = value,
                    data -> data.zoneId
            )
            .addField(
                    new KeyedCodec<>("ZoneUuid", Codec.STRING),
                    (data, value) -> data.zoneUuid = value,
                    data -> data.zoneUuid
            )
            .addField(
                    new KeyedCodec<>("ZoneName", Codec.STRING),
                    (data, value) -> data.zoneName = value,
                    data -> data.zoneName
            )
            .addField(
                    new KeyedCodec<>("ZoneType", Codec.STRING),
                    (data, value) -> data.zoneType = value,
                    data -> data.zoneType
            )
            .addField(
                    new KeyedCodec<>("ChunkX", Codec.STRING),
                    (data, value) -> {
                        try {
                            data.chunkX = value != null ? Integer.parseInt(value) : 0;
                        } catch (NumberFormatException e) {
                            data.chunkX = 0;
                        }
                    },
                    data -> String.valueOf(data.chunkX)
            )
            .addField(
                    new KeyedCodec<>("ChunkZ", Codec.STRING),
                    (data, value) -> {
                        try {
                            data.chunkZ = value != null ? Integer.parseInt(value) : 0;
                        } catch (NumberFormatException e) {
                            data.chunkZ = 0;
                        }
                    },
                    data -> String.valueOf(data.chunkZ)
            )
            .addField(
                    new KeyedCodec<>("Page", Codec.STRING),
                    (data, value) -> {
                        try {
                            data.page = value != null ? Integer.parseInt(value) : 0;
                        } catch (NumberFormatException e) {
                            data.page = 0;
                        }
                    },
                    data -> String.valueOf(data.page)
            )
            .addField(
                    new KeyedCodec<>("@Name", Codec.STRING),
                    (data, value) -> data.inputName = value,
                    data -> data.inputName
            )
            .addField(
                    new KeyedCodec<>("ClaimMethod", Codec.STRING),
                    (data, value) -> data.claimMethod = value,
                    data -> data.claimMethod
            )
            .addField(
                    new KeyedCodec<>("Radius", Codec.STRING),
                    (data, value) -> data.radius = value,
                    data -> data.radius
            )
            .addField(
                    new KeyedCodec<>("@CustomRadius", Codec.STRING),
                    (data, value) -> data.customRadius = value,
                    data -> data.customRadius
            )
            .addField(
                    new KeyedCodec<>("FlagsChoice", Codec.STRING),
                    (data, value) -> data.flagsChoice = value,
                    data -> data.flagsChoice
            )
            .addField(
                    new KeyedCodec<>("@SortMode", Codec.STRING),
                    (data, value) -> data.sortMode = value,
                    data -> data.sortMode
            )
            .addField(
                    new KeyedCodec<>("AdminNavBar", Codec.STRING),
                    (data, value) -> data.adminNavBar = value,
                    data -> data.adminNavBar
            )
            .build();

    public AdminZoneData() {
    }

    @Override
    @Nullable
    public String getAdminNavBar() {
        return adminNavBar;
    }
}
