package com.platisa.app.core.domain.repository

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import java.io.File

interface GmailRepository {
    suspend fun fetchReceipts(account: GoogleSignInAccount, ignoreTimestamp: Boolean = false, lookbackDays: Int? = null): List<File>
}

