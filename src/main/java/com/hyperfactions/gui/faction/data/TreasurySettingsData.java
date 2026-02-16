package com.hyperfactions.gui.faction.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Treasury settings sub-page.
 */
public class TreasurySettingsData {

    /** The button/action that triggered the event */
    public String button;

    /** Permission flag name (for toggle actions) */
    public String perm;

    /** Limit configuration fields */
    public String maxWithdrawAmount;
    public String maxWithdrawPeriod;
    public String maxTransferAmount;
    public String maxTransferPeriod;
    public String periodHours;

    /** Codec for serialization/deserialization */
    public static final BuilderCodec<TreasurySettingsData> CODEC = BuilderCodec
            .builder(TreasurySettingsData.class, TreasurySettingsData::new)
            .addField(
                    new KeyedCodec<>("Button", Codec.STRING),
                    (data, value) -> data.button = value,
                    data -> data.button
            )
            .addField(
                    new KeyedCodec<>("Perm", Codec.STRING),
                    (data, value) -> data.perm = value,
                    data -> data.perm
            )
            .addField(
                    new KeyedCodec<>("@MaxWithdrawAmount", Codec.STRING),
                    (data, value) -> data.maxWithdrawAmount = value,
                    data -> data.maxWithdrawAmount
            )
            .addField(
                    new KeyedCodec<>("@MaxWithdrawPeriod", Codec.STRING),
                    (data, value) -> data.maxWithdrawPeriod = value,
                    data -> data.maxWithdrawPeriod
            )
            .addField(
                    new KeyedCodec<>("@MaxTransferAmount", Codec.STRING),
                    (data, value) -> data.maxTransferAmount = value,
                    data -> data.maxTransferAmount
            )
            .addField(
                    new KeyedCodec<>("@MaxTransferPeriod", Codec.STRING),
                    (data, value) -> data.maxTransferPeriod = value,
                    data -> data.maxTransferPeriod
            )
            .addField(
                    new KeyedCodec<>("@PeriodHours", Codec.STRING),
                    (data, value) -> data.periodHours = value,
                    data -> data.periodHours
            )
            .build();

    public TreasurySettingsData() {
    }
}
