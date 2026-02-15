package com.hyperfactions.gui.admin.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.jetbrains.annotations.Nullable;

/**
 * Event data for the Admin Player Info page.
 */
public class AdminPlayerInfoData implements AdminNavAwareData {

    /** The button/action that triggered the event */
    public String button;

    /** Static delta value for +/- adjust buttons */
    public String delta;

    /** Dynamic text field value for Set/SetMax (read via @PowerInput) */
    public String powerInput;

    /** Admin nav bar target (for navigation) */
    public String adminNavBar;

    /** Codec for serialization/deserialization */
    public static final BuilderCodec<AdminPlayerInfoData> CODEC = BuilderCodec
            .builder(AdminPlayerInfoData.class, AdminPlayerInfoData::new)
            .addField(
                    new KeyedCodec<>("Button", Codec.STRING),
                    (data, value) -> data.button = value,
                    data -> data.button
            )
            .addField(
                    new KeyedCodec<>("Delta", Codec.STRING),
                    (data, value) -> data.delta = value,
                    data -> data.delta
            )
            .addField(
                    new KeyedCodec<>("@PowerInput", Codec.STRING),
                    (data, value) -> data.powerInput = value,
                    data -> data.powerInput
            )
            .addField(
                    new KeyedCodec<>("AdminNavBar", Codec.STRING),
                    (data, value) -> data.adminNavBar = value,
                    data -> data.adminNavBar
            )
            .build();

    public AdminPlayerInfoData() {
    }

    @Override
    @Nullable
    public String getAdminNavBar() {
        return adminNavBar;
    }
}
