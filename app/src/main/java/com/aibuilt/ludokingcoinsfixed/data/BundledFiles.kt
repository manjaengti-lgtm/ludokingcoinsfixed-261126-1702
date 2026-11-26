package com.aibuilt.ludokingcoinsfixed.data

data class BundledFile(
    val id: String,
    val assetName: String,
    val fileName: String,
    val about: String
)

object BundledFiles {
    val all: List<BundledFile> = listOf(
        BundledFile(
            id = "patch_apk",
            assetName = "patch_apk.py",
            fileName = "patch_apk.py",
            about = "Applies the three coin byte patches to libil2cpp.so, keeping every zip entry and its compression."
        ),
        BundledFile(
            id = "verify_sh",
            assetName = "verify.sh",
            fileName = "verify.sh",
            about = "The finishing checks: entry count, manifest hash, split attributes, zipalign, signature, patched bytes."
        ),
        BundledFile(
            id = "readme",
            assetName = "README.md",
            fileName = "README.md",
            about = "Step by step rebuild: merge, patch, align, sign with the Arin key, verify, rename the output."
        )
    )

    fun byId(id: String): BundledFile? = all.firstOrNull { it.id == id }
}
