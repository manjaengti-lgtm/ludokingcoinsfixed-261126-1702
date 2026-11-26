package com.aibuilt.ludokingcoinsfixed.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aibuilt.ludokingcoinsfixed.model.PatchSpec
import com.aibuilt.ludokingcoinsfixed.ui.components.MonoBlock
import com.aibuilt.ludokingcoinsfixed.ui.components.SectionCard

private data class Step(val title: String, val body: String, val commands: String?)

private val steps = listOf(
    Step(
        title = "1 · Merge the four stock files",
        body = "Fold the config, UnityDataAssetPack and AddressablesAssetPack splits into the base APK. " +
            "All 2131 base entries must survive, including every assets/bin/Data file and global-metadata.dat.",
        commands = null
    ),
    Step(
        title = "2 · Patch the library",
        body = "Only lib/arm64-v8a/libil2cpp.so is written. RVAs are turned into file offsets through the " +
            "PT_LOAD program headers, so the ELF stays valid.",
        commands = "python3 patch_apk.py stock-base.apk ${PatchSpec.OUTPUT_NAME}"
    ),
    Step(
        title = "3 · Align",
        body = "The finished APK is page aligned.",
        commands = "zipalign -p -f 4 ${PatchSpec.OUTPUT_NAME} aligned.apk"
    ),
    Step(
        title = "4 · Sign with the Arin key",
        body = "v2 and v3 are on, v1 is off, so no unsigned META-INF is left behind.",
        commands = "apksigner sign --ks arin.keystore --ks-pass pass:arin \\\n" +
            "  --v1-signing-enabled false --v2-signing-enabled true --v3-signing-enabled true \\\n" +
            "  aligned.apk"
    ),
    Step(
        title = "5 · Verify",
        body = "Run the bundled verify script until every line says OK.",
        commands = "./verify.sh ${PatchSpec.OUTPUT_NAME} stock-base.apk"
    ),
    Step(
        title = "6 · Install",
        body = "Different signing key than the Play build, so uninstall Ludo King first. Uninstalling can " +
            "erase local progress — only do it on a Google or Facebook linked account, or accept the loss. " +
            "64-bit device only, and adb install -r will not work over the Play build.",
        commands = "adb uninstall ${PatchSpec.PACKAGE_NAME}\nadb install ${PatchSpec.OUTPUT_NAME}"
    )
)

@Composable
fun GuideScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionCard(title = "Signing identity") {
                Text(PatchSpec.SIGNER, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Key generation:",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                MonoBlock(
                    "keytool -genkeypair -v -keystore arin.keystore -alias arin \\\n" +
                        "  -keyalg RSA -keysize 2048 -validity 10000 \\\n" +
                        "  -dname \"${PatchSpec.SIGNER}\" -storepass arin -keypass arin"
                )
            }
        }
        items(steps) { step ->
            SectionCard(title = step.title) {
                Text(step.body, style = MaterialTheme.typography.bodyMedium)
                step.commands?.let { command ->
                    MonoBlock(command, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        item {
            SectionCard(title = "Honest limits") {
                Text(
                    text = "The server keeps its own ledger. The client reads a fixed balance, so play and " +
                        "local stake checks stay unlimited, but the backend never records the big number. " +
                        "A modified, re-signed client can trip Play Integrity and can be flagged server-side. " +
                        "Nothing here changes gems, XP or the game's own menu.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item {
            SectionCard(title = "Definition of done") {
                Text(
                    text = "2131 entries, manifest hash equal to stock, zipalign clean, signature verifying " +
                        "on v2/v3, patched bytes present at all three offsets, no split attributes anywhere, " +
                        "extractNativeLibs still true.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
