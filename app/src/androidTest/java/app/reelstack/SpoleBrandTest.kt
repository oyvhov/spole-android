package app.reelstack

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class SpoleBrandTest {
    @Test fun appIdentityAndAdaptiveLayersRenderAtLauncherAndSmallSizes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("Spole", context.getString(R.string.app_name))
        val icon = context.getDrawable(R.mipmap.ic_spole) as AdaptiveIconDrawable
        assertNotNull(icon.background)
        assertNotNull(icon.foreground)
        if (android.os.Build.VERSION.SDK_INT >= 33) assertNotNull(icon.monochrome)
        for (size in listOf(24, 48, 512)) {
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            icon.setBounds(0, 0, size, size)
            icon.draw(Canvas(bitmap))
            var limePixels = 0
            for (y in 0 until size) for (x in 0 until size) {
                val color = bitmap.getPixel(x, y)
                if (android.graphics.Color.alpha(color) > 200 && android.graphics.Color.green(color) > 180) limePixels++
            }
            assertTrue("The Spole mark must remain visible at $size px", limePixels > size)
            if (size == 512) File(context.getExternalFilesDir(null), "spole-icon-preview.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        }
    }
}
