package com.hyperfactions.gui.newplayer.data;

import com.hyperfactions.gui.shared.data.NavAwareData;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data class for New Player GUI pages.
 * Includes fields for browse, create wizard, invites, map, and help pages.
 */
public class NewPlayerPageData implements NavAwareData {

    /** The button/action that triggered the event */
    public String button;

    /** NavBar target (page ID when nav button clicked) */
    public String navBar;

    /** Current page number for paginated lists (0-indexed) */
    public int page;

    /** Faction ID for faction-specific actions (join, view, etc.) */
    public String factionId;

    /** Faction name (for display/search) */
    public String factionName;

    /** Sort mode for lists (e.g., "power", "members", "name") */
    public String sortMode;

    /** Search query for filtered lists */
    public String searchQuery;

    // === Create Wizard Fields ===

    /** Faction name input from Step 1 */
    public String inputName;

    /** Selected color code from Step 1 */
    public String inputColor;

    /** Faction tag input from Step 1 */
    public String inputTag;

    /** Description input from Step 2 */
    public String inputDescription;

    /** Recruitment setting (true = open, false = invite only) */
    public String inputRecruitment;

    /** Permission name for toggle events */
    public String perm;

    // === Codec ===

    public static final BuilderCodec<NewPlayerPageData> CODEC = BuilderCodec
            .builder(NewPlayerPageData.class, NewPlayerPageData::new)
            .addField(
                    new KeyedCodec<>("Button", Codec.STRING),
                    (data, value) -> data.button = value,
                    data -> data.button
            )
            .addField(
                    new KeyedCodec<>("NavBar", Codec.STRING),
                    (data, value) -> data.navBar = value,
                    data -> data.navBar
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
                    new KeyedCodec<>("FactionId", Codec.STRING),
                    (data, value) -> data.factionId = value,
                    data -> data.factionId
            )
            .addField(
                    new KeyedCodec<>("FactionName", Codec.STRING),
                    (data, value) -> data.factionName = value,
                    data -> data.factionName
            )
            .addField(
                    new KeyedCodec<>("@SortMode", Codec.STRING),
                    (data, value) -> data.sortMode = value,
                    data -> data.sortMode
            )
            .addField(
                    new KeyedCodec<>("@SearchQuery", Codec.STRING),
                    (data, value) -> data.searchQuery = value,
                    data -> data.searchQuery
            )
            // Create wizard input fields (@ prefix for input binding)
            .addField(
                    new KeyedCodec<>("@Name", Codec.STRING),
                    (data, value) -> data.inputName = value,
                    data -> data.inputName
            )
            .addField(
                    new KeyedCodec<>("@Color", Codec.STRING),
                    (data, value) -> data.inputColor = value,
                    data -> data.inputColor
            )
            .addField(
                    new KeyedCodec<>("@Tag", Codec.STRING),
                    (data, value) -> data.inputTag = value,
                    data -> data.inputTag
            )
            .addField(
                    new KeyedCodec<>("@Description", Codec.STRING),
                    (data, value) -> data.inputDescription = value,
                    data -> data.inputDescription
            )
            .addField(
                    new KeyedCodec<>("@Recruitment", Codec.STRING),
                    (data, value) -> data.inputRecruitment = value,
                    data -> data.inputRecruitment
            )
            .addField(
                    new KeyedCodec<>("Perm", Codec.STRING),
                    (data, value) -> data.perm = value,
                    data -> data.perm
            )
            .build();

    public NewPlayerPageData() {
    }

    @Override
    public String getNavBar() {
        return navBar;
    }

    @Override
    public String toString() {
        return "NewPlayerPageData{" +
                "button='" + button + '\'' +
                ", navBar='" + navBar + '\'' +
                ", page=" + page +
                ", factionId='" + factionId + '\'' +
                ", factionName='" + factionName + '\'' +
                ", sortMode='" + sortMode + '\'' +
                ", searchQuery='" + searchQuery + '\'' +
                ", inputName='" + inputName + '\'' +
                ", inputColor='" + inputColor + '\'' +
                ", inputTag='" + inputTag + '\'' +
                ", inputDescription='" + inputDescription + '\'' +
                ", inputRecruitment='" + inputRecruitment + '\'' +
                ", perm='" + perm + '\'' +
                '}';
    }
}
