package world.engine.asseteditor

import android.net.Uri
import world.engine.assets.*

/** Editor-facing actions. All disk-backed implementations dispatch away from the UI thread. */
interface AssetActions {
    fun selectAsset(id: String)
    fun useAsset(id: String)
    fun importAsset(uri: Uri)
    fun replaceAsset(id: String,uri: Uri)
    fun metadata(id: String,name: String,folder: String,tags: List<String>,favorite: Boolean)
    fun prefabFromAsset(id: String)
    fun duplicateAsset(id: String)
    fun deleteAsset(id: String)
    fun editTexture(id: String,edit: TextureEdit)
    fun autoSlice(id: String)
    fun setFrames(frames: List<SpriteFrame>)
    fun appendFrame(frame: SpriteFrame)
    fun saveSheet(id: String)
    fun exportFrames(id: String,uri: Uri)
    fun extractFrames(id: String)
    fun splitAsset(id: String)
    fun exportAsset(id: String,uri: Uri)
    fun generateLibrary()
    fun assetError(message: String)
}
