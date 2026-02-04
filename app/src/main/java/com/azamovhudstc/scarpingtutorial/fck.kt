package com.azamovhudstc.scarpingtutorial

import com.azamovhudstc.scarpingtutorial.utils.GitHubUnfollower
import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.lagradost.nicehttp.Requests
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

fun main(args: Array<String>) {
    runBlocking {
        withContext(Dispatchers.IO) {
            val success = GitHubUnfollower.unfollowAllUsers()

            if (success) {
                println("All users unfollowed successfully!")
            } else {
                println("There were some errors during the unfollow process.")
            }
        }

    }
}
