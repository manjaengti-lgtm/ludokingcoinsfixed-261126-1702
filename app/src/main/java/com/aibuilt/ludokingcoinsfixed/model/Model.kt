package com.aibuilt.ludokingcoinsfixed.model

import kotlinx.serialization.Serializable

@Serializable
data class LedgerEntry(
    val amount: Long,
    val note: String,
    val timestamp: Long
)

data class PatchSite(
    val rva: String,
    val method: String,
    val bytes: String,
    val instructions: String,
    val effect: String
)

data class CheckItem(
    val id: String,
    val label: String,
    val detail: String
)

object PatchSpec {
    const val PACKAGE_NAME = "com.ludo.king"
    const val VERSION_NAME = "10.5.0.406"
    const val VERSION_CODE = 406
    const val TARGET_SDK = 36
    const val MIN_SDK = 24
    const val ABI = "arm64-v8a"
    const val OUTPUT_NAME = "LudoKing-coins-FIXED.apk"
    const val ENTRY_COUNT = 2131
    const val SIGNER = "CN=Arin, OU=Arin, O=Arin, L=Arin, ST=Arin, C=IN"
    const val LIBRARY = "lib/arm64-v8a/libil2cpp.so"
    const val COIN_VALUE = 999_999_999L
    const val COIN_HEX = "0x3B9AC9FF"
    const val SPEC_DECIMAL = 1_929_256_944L
    const val UNTOUCHED_RVA = "0x21B9E6C"
    const val UNTOUCHED_METHOD = "ThirdParties.SetChips(long)"

    val sites: List<PatchSite> = listOf(
        PatchSite(
            rva = "0x21B9E00",
            method = "ThirdParties.GetChips()",
            bytes = "e03f99d2 4073a7f2 0000c0f2",
            instructions = "movz x0,#0xc9ff ; movk x0,#0x3b9a,lsl #16 ; movk x0,#0,lsl #32",
            effect = "Returns the fixed balance from the first chips getter, so the displayed balance never drops."
        ),
        PatchSite(
            rva = "0x22DDC0C",
            method = "second GetChips()",
            bytes = "e03f99d2 4073a7f2 0000c0f2",
            instructions = "movz x0,#0xc9ff ; movk x0,#0x3b9a,lsl #16 ; movk x0,#0,lsl #32",
            effect = "Same fixed balance from the second getter, so both call paths agree."
        ),
        PatchSite(
            rva = "0x20CA650",
            method = "IsEnoughCoinsAvailableToPlay()",
            bytes = "20008052 c0035fd6",
            instructions = "mov w0,#1 ; ret",
            effect = "Every stake check reads as affordable, so no table is ever blocked for coins."
        )
    )

    val checks: List<CheckItem> = listOf(
        CheckItem(
            id = "entries",
            label = "Entry count is 2131",
            detail = "unzip -l reports 2131 entries — the stock base entry list, zero diff."
        ),
        CheckItem(
            id = "manifest_hash",
            label = "Manifest hash equals stock",
            detail = "sha256 of AndroidManifest.xml in the build matches the stock base."
        ),
        CheckItem(
            id = "no_split",
            label = "No split attributes in the manifest",
            detail = "No split= name, no isSplitRequired, no requiredSplitTypes / splitTypes."
        ),
        CheckItem(
            id = "extract_native",
            label = "extractNativeLibs stays true",
            detail = "Native libraries still unpack on install."
        ),
        CheckItem(
            id = "deflate",
            label = "libil2cpp.so is stored Defl:N",
            detail = "The patched library keeps the stock compression method."
        ),
        CheckItem(
            id = "bytes",
            label = "All three byte patches present",
            detail = "Patched bytes verified at 0x21B9E00, 0x22DDC0C and 0x20CA650."
        ),
        CheckItem(
            id = "setchips",
            label = "SetChips left untouched",
            detail = "0x21B9E6C has no edits, so the forced getters sit on top of the stock setter."
        ),
        CheckItem(
            id = "align",
            label = "zipalign -c -p -v 4 passes",
            detail = "Alignment checked after the rebuild."
        ),
        CheckItem(
            id = "sign",
            label = "apksigner verify passes on v2/v3",
            detail = "Signed with the Arin key, v1 may be off, no unsigned META-INF left over."
        )
    )
}
