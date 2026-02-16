package com.hyperfactions.gui.help.data;

import com.hyperfactions.gui.shared.data.NavAwareData;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data class for Help GUI pages.
 * Handles category selection and navigation.
 */
public class HelpPageData implements NavAwareData {

    /** The button/action that triggered the event */
    public String button;

    /** Selected category ID for category switching */
    public String category;

    /** Optional topic ID for direct topic navigation */
    public String topic;

    /** NavBar target (page ID when nav button clicked) */
    public String navBar;

    /** Codec for serialization/deserialization */
    public static final BuilderCodec<HelpPageData> CODEC = BuilderCodec
            .builder(HelpPageData.class, HelpPageData::new)
            .addField(
                    new KeyedCodec<>("Button", Codec.STRING),
                    (data, value) -> data.button = value,
                    data -> data.button
            )
            .addField(
                    new KeyedCodec<>("Category", Codec.STRING),
                    (data, value) -> data.category = value,
                    data -> data.category
            )
            .addField(
                    new KeyedCodec<>("Topic", Codec.STRING),
                    (data, value) -> data.topic = value,
                    data -> data.topic
            )
            .addField(
                    new KeyedCodec<>("NavBar", Codec.STRING),
                    (data, value) -> data.navBar = value,
                    data -> data.navBar
            )
            .build();

    public HelpPageData() {
    }

    @Override
    public String getNavBar() {
        return navBar;
    }

    @Override
    public String toString() {
        return "HelpPageData{" +
                "button='" + button + '\'' +
                ", category='" + category + '\'' +
                ", topic='" + topic + '\'' +
                ", navBar='" + navBar + '\'' +
                '}';
    }
}
