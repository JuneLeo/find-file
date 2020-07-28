package com.cheetah.kotlin

import android.Manifest
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    var adapter: FileAdapter = FileAdapter()
    var totalSize: Long = 0;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        val recyclerView = findViewById<RecyclerView>(R.id.recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

    }

    class FileAdapter : Adapter<CommonVH>() {
        private val items: MutableList<BaseModel> = mutableListOf()

        fun getItem(): MutableList<BaseModel> {
            return items
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommonVH {
            if (viewType == BaseModel.TYPE_FILE) {
                return FileVH(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_file, parent, false)
                )
            }
            throw RuntimeException("")
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onBindViewHolder(holder: CommonVH, position: Int) {
            if (holder is FileVH) {
                holder.update(items[position] as FileModel, position)
            }
        }

        fun update(item: List<BaseModel>) {
            this.items.clear()
            this.items.addAll(item)
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int {
            return items[position].type
        }


        inner class FileVH(itemView: View) : CommonVH(itemView) {
            private var tvName: TextView? = null
            private var tvSize: TextView? = null
            var tvDelete: TextView

            init {
                tvName = itemView.findViewById(R.id.tv_name)
                tvSize = itemView.findViewById(R.id.tv_size)
                tvDelete = itemView.findViewById(R.id.tv_delete)
            }

            fun update(t: FileModel, position: Int) {
                tvName?.setText(t.name)
                tvSize?.setText(t.size.toString())
                tvDelete.setOnClickListener {
                    items.removeAt(position)
                    notifyDataSetChanged()
                }


            }


        }


    }


    class FileModel(var name: String, var size: Long) : BaseModel() {
        init {
            type = TYPE_FILE
        }

        var leo: Int = 1

        inner class a {

            fun a() {
                leo
            }
        }
    }

    abstract class BaseModel {
        var type: Int = 0

        companion object {
            const val TYPE_FILE: Int = 1
        }


    }


    abstract class CommonVH(itemView: View) : RecyclerView.ViewHolder(itemView)


    fun requestPermission(view: View) {

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            , 203
        )
    }

    var fileScanner: FileScanner? = null

    var fileItem: MutableList<FileModel> = mutableListOf()

    fun onScan(view: View) {
        fileItem.clear()
        totalSize = 0;
        findViewById<View>(R.id.tv_scan).isEnabled = false
        fileScanner = FileScanner()
        fileScanner?.scan(this, object : Callback {
            override fun onStart() {
                Toast.makeText(this@MainActivity, "扫描开始", Toast.LENGTH_SHORT).show()
            }

            override fun onScanner(files: List<File>) {
                val map = files.map { FileModel(it.absolutePath, it.length()) }

                insertList(map)

                if (!handler.hasMessages(REFRESH)) {
                    handler.sendEmptyMessageDelayed(REFRESH, 3000)
                }
            }

            override fun onEnd() {
                runOnUiThread(Runnable {
                    findViewById<View>(R.id.tv_scan).isEnabled = true
                    Toast.makeText(this@MainActivity, "扫描完成", Toast.LENGTH_SHORT).show()
                })
            }

            override fun onError(e: Exception) {
                (Runnable {
                    findViewById<View>(R.id.tv_scan).isEnabled = true
                    Toast.makeText(this@MainActivity, e.message ?: "发生错误", Toast.LENGTH_SHORT)
                        .show()
                })
            }

        })
    }

    var handler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                REFRESH -> {
                    adapter.update(fileItem)
                    findViewById<TextView>(R.id.tv_request).setText(
                        "$totalSize,格式化：${String.format(
                            "%.2fMB",
                            (totalSize.toFloat() / 1024 / 1024)
                        )}"
                    )
                }
            }
        }
    }

    var executor: ExecutorService = Executors.newFixedThreadPool(4)

    private fun insertList(map: List<FileModel>) {
        map.forEach {
            synchronized(fileItem) {
                totalSize += it.size

                var start = 0
                var end: Int = fileItem.size - 1
                var middle = 0

                while (start <= end) {
                    val size: Long = it.size
                    middle = (end + start) / 2
                    val middleSize: Long = fileItem[middle].size
                    if (size == middleSize) {
                        break
                    } else if (size < middleSize) {
                        start = middle + 1
                        middle = start
                    } else {
                        end = middle - 1
                    }
                }
                fileItem.add(middle, it)
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_log) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(this, "没有权限", Toast.LENGTH_LONG).show()
                return true
            }

            val items = adapter.getItem();
            if (items.isEmpty()) {
                return true
            }
            val pathFile = Environment.getExternalStoragePublicDirectory("0/file-path.txt")
            if (!pathFile.parentFile.exists()) {
                pathFile.parentFile.mkdirs()
            }

            if (pathFile.exists()) {
                pathFile.delete()
            }


            Toast.makeText(this, "正在写入到${pathFile.absolutePath}", Toast.LENGTH_LONG).show()

            Thread(Runnable {

                var fos: FileWriter? = null
                try {
                    fos = FileWriter(pathFile)
                    (items.filter {
                        it is FileModel
                    } as List<FileModel>).forEach {
                        var newName: String = it.name
                        fos.write(newName.replace("/storage/emulated/0",""))
                        fos.write("\t")
                        fos.write(it.size.toString())
                        fos.write("\n")
                    }
                    fos.flush()

                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    fos?.close()
                }


                runOnUiThread(Runnable {
                    Toast.makeText(this, "写入完成", Toast.LENGTH_LONG).show()
                })
            }).start()


        }
        return super.onOptionsItemSelected(item)
    }

    fun onStop(view: View) {
        fileScanner?.stop()
        findViewById<View>(R.id.tv_scan).isEnabled = true
    }

    companion object {
        const val REFRESH: Int = 1;
    }


}
