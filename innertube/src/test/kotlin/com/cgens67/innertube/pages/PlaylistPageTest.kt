package com.cgens67.innertube.pages

import com.cgens67.innertube.models.Artist
import com.cgens67.innertube.models.Run
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistPageTest {
    @Test
    fun `legacy detail subtitle has an owner in its second section`() {
        assertEquals(
            Artist("Owner & Friends", null),
            PlaylistPage.ownerFromDetailSubtitle(
                listOf(
                    Run("Playlist", null),
                    Run(" • ", null),
                    Run("Owner & Friends", null),
                    Run(" • ", null),
                    Run("14 songs", null),
                ),
            ),
        )
        assertNull(PlaylistPage.ownerFromDetailSubtitle(listOf(Run("canción", null))))
        assertNull(PlaylistPage.ownerFromDetailSubtitle(listOf(Run("Playlist", null), Run(" • ", null), Run(" ", null))))
    }
}