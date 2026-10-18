package com.FrancescoCardone

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class SkipIntroPlugin : Plugin() {

    private var lastClickTime = 0L

    override fun load(context: Context) {
        val app = context.applicationContext as? Application ?: return

        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                val rootView = activity.window?.decorView?.rootView ?: return

                // Aggiunge un listener globale al layout dell'Activity attiva
                rootView.viewTreeObserver.addOnGlobalLayoutListener(
                    object : ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            if (activity.isDestroyed || activity.isFinishing) {
                                rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                                return
                            }
                            checkAndClickSkip(rootView, activity)
                        }
                    }
                )
            }

            override fun onActivityCreated(a: Activity, b: Bundle?) {}
            override fun onActivityStarted(a: Activity) {}
            override fun onActivityPaused(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })
    }

    private fun checkAndClickSkip(root: View, activity: Activity) {
        val now = System.currentTimeMillis()
        // Cooldown di 3 secondi per evitare doppi click a raffica
        if (now - lastClickTime < 3000) return

        // 1. Ricerca tramite gli ID interni usati da CloudStream per i capitoli/intro
        val targetIds = listOf(
            "skip_chapter_button",
            "player_skip_episode",
            "exo_skip_button"
        )

        for (idName in targetIds) {
            val resId = activity.resources.getIdentifier(idName, "id", activity.packageName)
            if (resId != 0) {
                val btn = root.findViewById<View>(resId)
                if (btn != null && btn.visibility == View.VISIBLE && btn.isShown && btn.isClickable) {
                    if (isIntroSkipButton(btn)) {
                        lastClickTime = now
                        btn.performClick()
                        return
                    }
                }
            }
        }

        // 2. Fallback: ricerca ricorsiva per testo nel caso cambi l'ID in aggiornamenti futuri
        findAndClickByText(root, now)
    }

    private fun isIntroSkipButton(view: View): Boolean {
        if (view is TextView) {
            val text = view.text?.toString()?.lowercase() ?: ""
            // Evita di cliccare "Prossimo episodio" se vogliamo saltare solo l'intro/opening
            return text.contains("intro") ||
                   text.contains("opening") ||
                   text.contains("op") ||
                   text.contains("sigla") ||
                   text.contains("salta") ||
                   text.contains("skip")
        }
        return true
    }

    private fun findAndClickByText(view: View, now: Long): Boolean {
        if (view.visibility != View.VISIBLE || !view.isShown) return false

        if (view is TextView) {
            val text = view.text?.toString()?.lowercase()?.trim() ?: ""
            val isSkipIntro = text == "skip intro" ||
                              text == "salta intro" ||
                              text == "skip opening" ||
                              text == "skip op" ||
                              text == "salta sigla"

            if (isSkipIntro) {
                val clickableTarget = if (view.isClickable) view else (view.parent as? View)
                if (clickableTarget != null && clickableTarget.isClickable) {
                    lastClickTime = now
                    clickableTarget.performClick()
                    return true
                }
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                if (findAndClickByText(view.getChildAt(i), now)) return true
            }
        }
        return false
    }
}
