package com.example.mysignmate

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LearnSignActivity : AppCompatActivity() {

    private lateinit var dictionaryAdapter: DictionaryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learn_sign)
        val recyclerView: RecyclerView = findViewById(R.id.list_dictionary)

        // Images and Videos sourced from https://www.bimsignbank.org/groups/
        val itemList = listOf(
            // Module 1
            DictionaryItem("Waalaikumussalam", R.drawable.waalaikumussalam,"https://www.youtube.com/embed/v0NRzhlaZJc?si=_MxSgMy-ecF_Hfxd"),
            DictionaryItem("Khabar Baik", R.drawable.khabar_baik,"https://www.youtube.com/embed/-EjOCS_PLt0?si=NPRZJGqJSHx-UY50"),
            DictionaryItem("Saya", R.drawable.saya,"https://www.youtube.com/embed/hFlubKThPdE?si=qPalsCtiq7g1g-3Y"),
            DictionaryItem("Awak", R.drawable.awak,"https://www.youtube.com/embed/GSERjhVh82E?si=LmKyC33bLW7Hyawk"),
            DictionaryItem("Nama", R.drawable.nama,"https://www.youtube.com/embed/0SPq5YPBvjI?si=INlI7wYqY1LwjfOr"),
            DictionaryItem("Bahasa Isyarat", R.drawable.bahasa_isyarat,"https://www.youtube.com/embed/ynMMpd4PmLs?si=KVVpDEo62nXwEHFu"),
            DictionaryItem("Terima Kasih", R.drawable.terima_kasih,"https://www.youtube.com/embed/uQiZ5mBhers?si=fxbUM984uCJYIbQG"),
            DictionaryItem("Selamat Pagi", R.drawable.selamat_pagi,"https://www.youtube.com/embed/kspVcgd9k-s?si=8vpLgvlfyaM_gJIe"),
            // Module 2
            DictionaryItem("Apa", R.drawable.apa,"https://www.youtube.com/embed/08zGcmBg824?si=J_yPbJXdXA0KzTGi"),
            DictionaryItem("Bagaimana", R.drawable.bagaimana,"https://www.youtube.com/embed/s6jw3JLtwo0?si=yZFfKjaWhWKIeV9y"),
            DictionaryItem("Baik", R.drawable.baik,"https://www.youtube.com/embed/DTUIaH_ab94?si=oH58l0U08jFM175A"),
            DictionaryItem("Berapa", R.drawable.berapa,"https://www.youtube.com/embed/rzEvFXzbVbM?si=xfJVNKWjNiPMsKnb"),
            DictionaryItem("Bila", R.drawable.bila,"https://www.youtube.com/embed/-VxAG25dK-A?si=IGLsp9c7G1n3j7XW"),
            DictionaryItem("Jahat", R.drawable.jahat,"https://www.youtube.com/embed/Osf4ve6tD-0?si=fqqkThHem90fwaPJ"),
            DictionaryItem("Mana", R.drawable.mana,"https://www.youtube.com/embed/LRjXzuyI2Ms?si=7xgAWergr4gPzoD3"),
            DictionaryItem("Panas", R.drawable.panas,"https://www.youtube.com/embed/P0YUZ4D2VL0?si=4lCOJ53si6EF60gM"),
            DictionaryItem("Panas2", R.drawable.panas2,"https://www.youtube.com/embed/jh9i3IjuQWY?si=ljesT8dDXxK_BjHw"),
            DictionaryItem("Pandai", R.drawable.pandai,"https://www.youtube.com/embed/IwFcVj8lszc?si=g-8DXMlbAtM5AtOh"),
            DictionaryItem("Pandai2", 0,"https://www.youtube.com/embed/Q8Df5bHxypA?si=ErlJLO17-ZRqBnSH"),
            DictionaryItem("Perlahan", R.drawable.perlahan,"https://www.youtube.com/embed/YovImQGhd5U?si=VPeHgbJym88ZJfGO"),
            DictionaryItem("Perlahan2", R.drawable.perlahan2,"https://www.youtube.com/embed/gDYekg6ZUyg?si=dSymoL-Bbu3-as3A"),
            DictionaryItem("Sejuk", R.drawable.sejuk,"https://www.youtube.com/embed/WlqnAFIuuR4?si=Cl0AEo7qKyaDOS2a"),
            DictionaryItem("Siapa", R.drawable.siapa,"https://www.youtube.com/embed/G7rz9r7afec?si=WPqbLxEUdSFEccKj"),
            // Module 3
            DictionaryItem("Abang", R.drawable.abang,"https://www.youtube.com/embed/LoGOEPehHa4?si=4D4Seme_ydG7x5Hw"),
            DictionaryItem("Ayah", R.drawable.ayah,"https://www.youtube.com/embed/387R2ObhNFk?si=axjxgwV0gIax7SC9"),
            DictionaryItem("Bomba", R.drawable.bomba,"https://www.youtube.com/embed/qi_tDJdJtCI?si=iKK66NkTKe62FlM1"),
            DictionaryItem("Emak", R.drawable.emak,"https://www.youtube.com/embed/xqtJtDP7zBE?si=iBiGCsy3tkUNp1dB"),
            DictionaryItem("Kakak", R.drawable.kakak,"https://www.youtube.com/embed/U4Mf_p9DM0E?si=i8wIW5il6N0V88rw"),
            DictionaryItem("Keluarga", R.drawable.keluarga,"https://www.youtube.com/embed/p32Gtmyz2Fs?si=7oiyYYR3VmRHQNqZ"),
            DictionaryItem("Lelaki", R.drawable.lelaki,"https://www.youtube.com/embed/rQOkFKoxPtc?si=Zwy5Edfb42djYWGo"),
            DictionaryItem("Perempuan", R.drawable.perempuan,"https://www.youtube.com/embed/vqWPssaQf9A?si=RqiNL4_y5ulpGu-R"),
            DictionaryItem("Polis", R.drawable.polis,"https://www.youtube.com/embed/xCWQ58uHY40?si=RXgsf2SNo4ftsCkT"),
            // Module 4
            DictionaryItem("Adik Lelaki", R.drawable.adik_lelaki,"https://www.youtube.com/embed/jA1p1ZZaIUo?si=_PA2G44EUIsoX5vu"),
            DictionaryItem("Adik Perempuan", R.drawable.adik_perempuan,"https://www.youtube.com/embed/yOHur9ac1Ds?si=KHU7f_mq_M5SMV7M"),
            DictionaryItem("Anak", R.drawable.anak,"https://www.youtube.com/embed/ZSyNrJtQBqM?si=-3hwT-fJH_v0BKMz"),
            DictionaryItem("Saudara", R.drawable.saudara,"https://www.youtube.com/embed/rFvVkxrnkGU?si=fAOpxlnZx2pPOLGu"),
            DictionaryItem("Datuk", R.drawable.datuk,"https://www.youtube.com/embed/NNrkBrEc0eA?si=Fi_1FwbIV3YIsKaP"),
            DictionaryItem("Nenek", R.drawable.nenek,"https://www.youtube.com/embed/dQUXKnhlOxY?si=uo1F1457CNmVxwr8"),
            // Module 5
            DictionaryItem("Air", R.drawable.air,"https://www.youtube.com/embed/6OcFfrXHt6Y?si=nPrw7Gdr4v7ffKLr"),
            DictionaryItem("Cuaca", R.drawable.cuaca,"https://www.youtube.com/embed/NbunuE17sdE?si=t0TPJ7nBGXbfHHDF"),
            DictionaryItem("Duit", R.drawable.duit,"https://www.youtube.com/embed/stL9uMAcgWw?si=2tbfuoRtOFh8xBKC"),
            DictionaryItem("Hari", R.drawable.hari,"https://www.youtube.com/embed/QnL-btp-Iwo?si=U7BPCNTgZMIGhueI"),
            DictionaryItem("Hujan", R.drawable.hujan,"https://www.youtube.com/embed/wmiBf3lYtsk?si=OMU0Ht0JAwx5pM70"),
            DictionaryItem("Masalah", R.drawable.masalah,"https://www.youtube.com/embed/Baexj1wZnII?si=sio0ZyknxJrWj1jW"),
            DictionaryItem("Payung", R.drawable.payung,"https://www.youtube.com/embed/qe230W7CmXQ?si=5VaQC51VUnLKmzl4"),
            DictionaryItem("Ribut", R.drawable.ribut,"https://www.youtube.com/embed/akDpVXtIQJ4?si=ntX8omIBQB3xpa2S"),
            DictionaryItem("Tandas", R.drawable.tandas,"https://www.youtube.com/embed/upxImdU-AgA?si=nomrPobJqNxbeSCm"),
            // Module 6
            DictionaryItem("Bas", R.drawable.bas,"https://www.youtube.com/embed/Bte27XdoOjQ?si=kmCHvAj4rPdKKWIq"),
            DictionaryItem("Bola", R.drawable.bola,"https://www.youtube.com/embed/3gG5Hg4gRZM?si=i2V0wcjPojpCn__D"),
            DictionaryItem("Esok", R.drawable.esok,"https://www.youtube.com/embed/DDhGVdBnYw0?si=i0FbZyTHJ6ahjhh_"),
            DictionaryItem("Jam", R.drawable.jam,"https://www.youtube.com/embed/LV76qMak38Y?si=AFgqB31YJAvAMwKs"),
            DictionaryItem("Kereta", R.drawable.kereta,"https://www.youtube.com/embed/NivM4IsyRfY?si=GCPXgGhME6aRKJwj"),
            DictionaryItem("Masa", 0,"https://www.youtube.com/embed/e8vPe7tiqJU?si=SBl517R7ZCnkpCMV"),
            DictionaryItem("Nasi Lemak", R.drawable.nasi_lemak,"https://www.youtube.com/embed/KhvAfNIrEs4?si=qk_WE2tklHwbIq3M"),
            DictionaryItem("Pensil", R.drawable.pensil,"https://www.youtube.com/embed/2HgLYRLaHJg?si=bkzkCmUYrrtIxtXD"),
            DictionaryItem("Pukul", R.drawable.pukul,"https://www.youtube.com/embed/BG7StzLGCvw?si=HLnwEyt_QavXEwFi"),
            DictionaryItem("Teh Tarik", R.drawable.teh_tarik,"https://www.youtube.com/embed/VxYCY9yihbI?si=4Eu4jFJc1BXIPiyj"),
            DictionaryItem("Teksi", R.drawable.teksi,"https://www.youtube.com/embed/kh10Kh7fOaI?si=e8c_BxSnJBceuJTn"),
            // Module 7
            DictionaryItem("Arah", R.drawable.arah,"https://www.youtube.com/embed/1Y2c-sYW5nI?si=CJkUUv3TD6FG8Hez"),
            DictionaryItem("Gambar", R.drawable.gambar,"https://www.youtube.com/embed/jkX0o8Ogy1Q?si=KxScP_4HO7WO0cgO"),
            DictionaryItem("Hospital", R.drawable.hospital,"https://www.youtube.com/embed/C4iZeB0Hw1c?si=cd6SBwl4YYHhw6Ov"),
            DictionaryItem("Jalan", R.drawable.jalan,"https://www.youtube.com/embed/DOEAS2wqZ5U?si=TT-OFqzZsmtidIv5"),
            DictionaryItem("Kafetaria", R.drawable.kafetaria,"https://www.youtube.com/embed/8M6IHcwyLIM?si=-y0nF6-NLGq-vksR"),
            DictionaryItem("Kedai", R.drawable.kedai,"https://www.youtube.com/embed/qZ6F_XW8eQ0?si=IZN_M0KDez54MkMP"),
            DictionaryItem("Keretapi", R.drawable.keretapi,"https://www.youtube.com/embed/hH_lzKNDdJE?si=hL46XRVA8V_4K5pp"),
            DictionaryItem("Makan", R.drawable.makan,"https://www.youtube.com/embed/TjRPpGqBfDM?si=LQe7p6bub15JWjNA"),
            DictionaryItem("Pen", R.drawable.pen,"https://www.youtube.com/embed/tf1Y7AzWuFs?si=lSj4fuwmei1YToJC"),
            DictionaryItem("Sekolah", R.drawable.sekolah,"https://www.youtube.com/embed/5AkfeDat-Q4?si=FrjKgKw3qdbsaqhH"),
//            // Module 8
//            DictionaryItem("Ada", R.drawable.,""),
//            DictionaryItem("Berlari", R.drawable.,""),
//            DictionaryItem("Boleh", R.drawable.,""),
//            DictionaryItem("Buang", R.drawable.,""),
//            DictionaryItem("Dapat", R.drawable.,""),
//            DictionaryItem("Dari", R.drawable.,""),
//            DictionaryItem("Minum", R.drawable.,""),
//            DictionaryItem("Mohon", R.drawable.,""),
//            DictionaryItem("Mohon2", R.drawable.,""),
//            DictionaryItem("Sudah", R.drawable.,""),
//            DictionaryItem("Tanya", R.drawable.,""),
//            DictionaryItem("Tolong", R.drawable.,""),
//            DictionaryItem("Tolong2", R.drawable.,""),
//            // Module 9
//            DictionaryItem("Bawa", R.drawable.,""),
//            DictionaryItem("Beli", R.drawable.,""),
//            DictionaryItem("Beli2", R.drawable.,""),
//            DictionaryItem("Buat", R.drawable.,""),
//            DictionaryItem("Jangan", R.drawable.,""),
//            DictionaryItem("Jumpa", R.drawable.,""),
//            DictionaryItem("Kacau", R.drawable.,""),
//            DictionaryItem("Lupa", R.drawable.,""),
//            DictionaryItem("Main", R.drawable.,""),
//            DictionaryItem("Marah", R.drawable.,""),
//            DictionaryItem("Marah2", R.drawable.,""),
//            DictionaryItem("Pinjam", R.drawable.,""),
//            DictionaryItem("Sampai", R.drawable.,""),
//            // Module 10
//            DictionaryItem("Ambil", R.drawable.,""),
//            DictionaryItem("Baca", R.drawable.,""),
//            DictionaryItem("Belajar", R.drawable.,""),
//            DictionaryItem("Berjalan", R.drawable.,""),
//            DictionaryItem("Curi", R.drawable.,""),
//            DictionaryItem("Hilang", R.drawable.,""),
//            DictionaryItem("Mari", R.drawable.,""),
//            DictionaryItem("Mari2", R.drawable.,""),
//            DictionaryItem("Kesakitan/Menyakitkan", R.drawable.,""),
//            DictionaryItem("Pergi", R.drawable.,""),
//            DictionaryItem("Pergi2", R.drawable.,""),
//            DictionaryItem("Suka", R.drawable.,""),
//            DictionaryItem("Tidur", R.drawable.,""),


//            DictionaryItem("A", R.drawable.a,"https://www.youtube.com/embed/mrtQKEIWYqc"),
//            DictionaryItem("B", R.drawable.b, "https://www.youtube.com/embed/LdM4S6BST0g"),
//            DictionaryItem("Bagaimana", R.drawable.bagaimana,"https://www.youtube.com/embed/s6jw3JLtwo0\\"),
//            DictionaryItem("C", R.drawable.c,"https://www.youtube.com/embed/Fbv-7HAS5wU"),
//            DictionaryItem("D", R.drawable.d,"https://www.youtube.com/embed/XKbgTKnANcY"),
//            DictionaryItem("E", R.drawable.e,"https://www.youtube.com/embed/Sxv8yrdfT9Q"),
//            DictionaryItem("F", R.drawable.f,"https://www.youtube.com/embed/AcSxG1__wxs"),
//            DictionaryItem("G", R.drawable.g,"https://www.youtube.com/embed/l-MF20_qOWs"),
//            DictionaryItem("H", R.drawable.h,"https://www.youtube.com/embed/UujVOdmfQNI"),
//            DictionaryItem("I", R.drawable.i,"https://www.youtube.com/embed/xGonomZ36bw"),
//            DictionaryItem("J", R.drawable.j,"https://www.youtube.com/embed/HeGhbHKOK9k"),
//            DictionaryItem("K", R.drawable.k,"https://www.youtube.com/embed/r6Ywn32kki8"),
//            DictionaryItem("Ke", R.drawable.ke,"https://www.youtube.com/embed/ZwtkTkmx5ME"),
//            DictionaryItem("L", R.drawable.l,"https://www.youtube.com/embed/Xn_VXjLL6Cc"),
//            DictionaryItem("M", R.drawable.m,"https://www.youtube.com/embed/-udLp7Vs_D8"),
//            DictionaryItem("N", R.drawable.n,"https://www.youtube.com/embed/2WugHgFRE9k"),
//            DictionaryItem("O", R.drawable.o,"https://www.youtube.com/embed/8SrQFU-OSC0"),
//            DictionaryItem("P", R.drawable.p,"https://www.youtube.com/embed/Zry4ncgRz7s"),
//            DictionaryItem("Perpustakaan", R.drawable.perpustakaan,"https://www.youtube.com/embed/yG12wX1g4Fk"),
//            DictionaryItem("Q", R.drawable.q,"https://www.youtube.com/embed/4UDPiFs3nys"),
//            DictionaryItem("R", R.drawable.r,"https://www.youtube.com/embed/HFNc88dL6wQ"),
//            DictionaryItem("S", R.drawable.s,"https://www.youtube.com/embed/jc58PaoNuiA"),
//            DictionaryItem("T", R.drawable.t,"https://www.youtube.com/embed/qoB09rjAo88"),
//            DictionaryItem("V", R.drawable.u,"https://www.youtube.com/embed/n861kdyURPQ"),
//            DictionaryItem("V", R.drawable.v,"https://www.youtube.com/embed/aQ94CfBI-oc"),
//            DictionaryItem("W", R.drawable.w,"https://www.youtube.com/embed/-P2m7bKLOgg"),
//            DictionaryItem("X", R.drawable.x,"https://www.youtube.com/embed/S_EV2-nJYPY"),
//            DictionaryItem("Y", R.drawable.y,"https://www.youtube.com/embed/0Ii73lt0pmk"),
//            DictionaryItem("Z", R.drawable.z,"https://www.youtube.com/embed/pnS4sNqK0yE")

            // Add more items as needed
        )


        // Create an instance of your Adapter class and attach it to the RecyclerView
        dictionaryAdapter = DictionaryAdapter(itemList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = dictionaryAdapter

        // Assuming you have a SearchView in your layout with ID search_view
        val searchView: SearchView = findViewById(R.id.search_view)

        // Set up SearchView listener
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                dictionaryAdapter.filter(newText.orEmpty())
                return true
            }
        })
    }
}