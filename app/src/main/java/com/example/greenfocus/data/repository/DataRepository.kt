package com.example.greenfocus.data.repository

import com.example.greenfocus.data.model.FocusSession
import com.example.greenfocus.di.FirebaseModule
import com.google.firebase.firestore.FirebaseFirestore

interface DataRepository {
    val database: FirebaseFirestore

    fun addSession(session: FocusSession)
    fun getSessions()
}

class ProdDataRepository : DataRepository{
    override val database = FirebaseModule.firestore
    override fun addSession(session: FocusSession) {
        TODO("Not yet implemented")
    }

    override fun getSessions() {
        TODO("Not yet implemented")
    }

}