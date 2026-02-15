package com.hyperfactions.gui.admin.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.jetbrains.annotations.Nullable;

/**
 * Event data for the Admin Players page.
 */
public class AdminPlayersData implements AdminNavAwareData {

    /** The button/action that triggered the event */
    public String button;

    /** Search query from text field (dynamic) */
    public String searchQuery;

    /** Sort mode from dropdown (dynamic) */
    public String sortMode;

    /** Target player UUID */
    public String playerUuid;

    /** Target player name */
    public String playerName;

    /** Page number for pagination */
    public int page;

    /** Admin nav bar target (for navigation) */
    public String adminNavBar;

    /** Codec for serialization/deserialization */
    public static final BuilderCodec<AdminPlayersData> CODEC = BuilderCodec
            .builder(AdminPlayersData.class, AdminPlayersData::new)
            .addField(
                    new KeyedCodec<>("Button", Codec.STRING),
                    (data, value) -> data.button = value,
                    data -> data.button
            )
            .addField(
                    new KeyedCodec<>("@SearchQuery", Codec.STRING),
                    (data, value) -> data.searchQuery = value,
                    data -> data.searchQuery
            )
            .addField(
                    new KeyedCodec<>("@SortMode", Codec.STRING),
                    (data, value) -> data.sortMode = value,
                    data -> data.sortMode
            )
            .addField(
                    new KeyedCodec<>("PlayerUuid", Codec.STRING),
                    (data, value) -> data.playerUuid = value,
                    data -> data.playerUuid
            )
            .addField(
                    new KeyedCodec<>("PlayerName", Codec.STRING),
                    (data, value) -> data.playerName = value,
                    data -> data.playerName
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
                    new KeyedCodec<>("AdminNavBar", Codec.STRING),
                    (data, value) -> data.adminNavBar = value,
                    data -> data.adminNavBar
            )
            .build();

    public AdminPlayersData() {
    }

    @Override
    @Nullable
    public String getAdminNavBar() {
        return adminNavBar;
    }
}
