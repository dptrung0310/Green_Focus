package com.example.greenfocus.data

import com.example.greenfocus.util.Sound

data class Ringtone(
    val id: Int = Sound.CLICK,
    val name: String = ""
)
object DataSource {
    val winRingtone = listOf(
        Ringtone(
            id = Sound.WIN_BELL,
            name = "Bell"
        ),
        Ringtone(
            id = Sound.WIN_APPLAUSE,
            name = "Applause"
        )
    )
}