package com.voicetype.keyboard

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class FeatureStoreActivity : Activity() {
    private lateinit var repository: FeatureStoreRepository
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = FeatureStoreRepository(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 24, 20, 20)
            setBackgroundColor(0xFFF7F9FD.toInt())
        }
        root.addView(TextView(this).apply {
            text = "Feature Store"
            textSize = 26f
            setTextColor(0xFF173B70.toInt())
        })
        root.addView(TextView(this).apply {
            text = "Offline theme packages • Verified data only"
            textSize = 13f
            setPadding(0, 6, 0, 16)
        })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list)
        setContentView(root)
        render()
    }

    private fun render() {
        list.removeAllViews()
        repository.catalog().forEach { pkg ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 12, 12, 12)
                setBackgroundColor(0xFFEAF0F8.toInt())
            }
            card.addView(TextView(this).apply {
                text = "${pkg.name}  •  ${pkg.category}"
                textSize = 18f
                setTextColor(0xFF173B70.toInt())
            })
            card.addView(TextView(this).apply {
                text = pkg.description
                textSize = 13f
            })
            val actions = LinearLayout(this).apply {
                gravity = Gravity.END
                orientation = LinearLayout.HORIZONTAL
            }
            if (repository.isInstalled(pkg.id)) {
                actions.addView(Button(this).apply {
                    text = "Apply"
                    setOnClickListener {
                        val ok = repository.applyTheme(pkg.id)
                        Toast.makeText(this@FeatureStoreActivity,
                            if (ok) "Theme applied" else "Could not apply theme",
                            Toast.LENGTH_SHORT).show()
                    }
                })
                actions.addView(Button(this).apply {
                    text = "Remove"
                    setOnClickListener {
                        repository.remove(pkg.id)
                        render()
                    }
                })
            } else {
                actions.addView(Button(this).apply {
                    text = "Install"
                    setOnClickListener {
                        val ok = repository.install(pkg)
                        Toast.makeText(this@FeatureStoreActivity,
                            if (ok) "Package verified and installed" else "Package rejected",
                            Toast.LENGTH_SHORT).show()
                        render()
                    }
                })
            }
            card.addView(actions)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
            list.addView(card, params)
        }
    }
}
