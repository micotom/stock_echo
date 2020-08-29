package com.funglejunk.stockecho.view

import android.content.Context
import android.graphics.*
import arrow.fx.IO
import timber.log.Timber

class ChartCanvas(
    context: Context
) : Canvas() {

    private val borderPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = context.resources.displayMetrics.density
        color = Color.GRAY
        pathEffect = DashPathEffect(floatArrayOf(3f, 12f), 50f)
    }

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = context.resources.displayMetrics.density * 1.5f
        color = Color.DKGRAY
    }

    private val path = Path()

    fun draw(
        data: List<Float>,
        viewWidth: Int,
        viewHeight: Int
    ): IO<Unit> = IO {
        drawLine(0f, 0f, viewWidth.toFloat(), 0f, borderPaint)
        drawLine(0f, viewHeight.toFloat(), viewWidth.toFloat(), viewHeight.toFloat(), borderPaint)

        if (data.isEmpty()) {
            return@IO
        }

        path.reset()
        path.moveTo(0f, 0f)
        val xSpreadFactor = viewWidth / data.size
        val normalisedData = data.yNormalise(viewHeight - 16)
        normalisedData.forEachIndexed { index, y ->
            Timber.d("drawing ${index * xSpreadFactor}, $y")
            val x = (index * xSpreadFactor).toFloat()
            if (index == 0) {
                path.moveTo(x, viewHeight - y)
            }
            path.lineTo(x, viewHeight - y)
            path.moveTo(x, viewHeight - y)
        }
        drawPath(path, paint)
    }

    private fun List<Float>.yNormalise(canvasHeight: Int) = run {
        val maxValueY = maxByOrNull { it }!!
        val minValueY = minByOrNull { it }!!
        val verticalSpan = when (val diff = maxValueY - minValueY) {
            0f -> 1f
            else -> diff
        }
        val factorY = canvasHeight / verticalSpan
        map {
            (it - minValueY) * factorY + 8
        }
    }

    /*
    companion object MockData {
        private val json = """
{"isin":"LU1737652823","data":[{"date":"2020-08-27","open":46.82,"close":47.295,"high":47.31,"low":46.645,"turnoverPieces":651,"turnoverEuro":30503.97},{"date":"2020-08-26","open":47.17,"close":47.065,"high":47.525,"low":47.065,"turnoverPieces":202,"turnoverEuro":9571.84},{"date":"2020-08-25","open":47.45,"close":46.98,"high":47.45,"low":46.98,"turnoverPieces":56,"turnoverEuro":2635.74},{"date":"2020-08-24","open":46.925,"close":46.895,"high":46.955,"low":46.75,"turnoverPieces":293,"turnoverEuro":13739.45},{"date":"2020-08-21","open":46.7,"close":46.79,"high":46.935,"low":46.7,"turnoverPieces":0,"turnoverEuro":0},{"date":"2020-08-20","open":45.83,"close":46.79,"high":46.79,"low":45.83,"turnoverPieces":1936,"turnoverEuro":89965.77},{"date":"2020-08-19","open":46.31,"close":46,"high":46.38,"low":46,"turnoverPieces":519,"turnoverEuro":23889.07},{"date":"2020-08-18","open":46.88,"close":46.28,"high":46.88,"low":46.28,"turnoverPieces":1010,"turnoverEuro":46819.33},{"date":"2020-08-17","open":46.815,"close":46.74,"high":46.815,"low":46.44,"turnoverPieces":1404,"turnoverEuro":65635.95},{"date":"2020-08-14","open":47.275,"close":46.835,"high":47.275,"low":46.61,"turnoverPieces":280,"turnoverEuro":13067.1},{"date":"2020-08-13","open":47.255,"close":47.03,"high":47.38,"low":47.03,"turnoverPieces":1603,"turnoverEuro":75877.99},{"date":"2020-08-12","open":47.435,"close":47.31,"high":47.47,"low":47.31,"turnoverPieces":132,"turnoverEuro":6262.71},{"date":"2020-08-11","open":47.715,"close":47.475,"high":47.91,"low":47.475,"turnoverPieces":3962,"turnoverEuro":189411.61},{"date":"2020-08-10","open":47.205,"close":47.49,"high":47.49,"low":47.02,"turnoverPieces":5745,"turnoverEuro":271733.12},{"date":"2020-08-07","open":46.425,"close":46.845,"high":46.895,"low":46.425,"turnoverPieces":394,"turnoverEuro":18448.52},{"date":"2020-08-06","open":46.305,"close":46.47,"high":46.68,"low":46.305,"turnoverPieces":944,"turnoverEuro":43964.33},{"date":"2020-08-05","open":46.775,"close":46.42,"high":46.95,"low":46.42,"turnoverPieces":2790,"turnoverEuro":129869.21},{"date":"2020-08-04","open":46.165,"close":46.85,"high":46.85,"low":46.165,"turnoverPieces":2766,"turnoverEuro":129205},{"date":"2020-08-03","open":46.515,"close":46.14,"high":46.69,"low":46.025,"turnoverPieces":4408,"turnoverEuro":204492.78},{"date":"2020-07-31","open":45.9,"close":45.77,"high":46.445,"low":45.77,"turnoverPieces":383,"turnoverEuro":17616.66},{"date":"2020-07-30","open":46.525,"close":46.335,"high":46.525,"low":45.645,"turnoverPieces":2499,"turnoverEuro":115490.52},{"date":"2020-07-29","open":46.09,"close":46.345,"high":46.625,"low":46.045,"turnoverPieces":568,"turnoverEuro":26335.94},{"date":"2020-07-28","open":45.12,"close":45.83,"high":45.83,"low":45.12,"turnoverPieces":668,"turnoverEuro":30300.51},{"date":"2020-07-27","open":45.77,"close":45.04,"high":45.775,"low":45,"turnoverPieces":447,"turnoverEuro":20364.85},{"date":"2020-07-24","open":45.505,"close":45.535,"high":46.05,"low":45.5,"turnoverPieces":5499,"turnoverEuro":251962.58},{"date":"2020-07-23","open":46.375,"close":46.08,"high":46.62,"low":46.08,"turnoverPieces":6477,"turnoverEuro":300678.7},{"date":"2020-07-22","open":46.06,"close":46.29,"high":46.29,"low":45.64,"turnoverPieces":585,"turnoverEuro":26803.11},{"date":"2020-07-21","open":46.61,"close":46.305,"high":46.61,"low":46.3,"turnoverPieces":15,"turnoverEuro":695.1},{"date":"2020-07-20","open":46.17,"close":46.04,"high":46.64,"low":46.04,"turnoverPieces":704,"turnoverEuro":32816.36},{"date":"2020-07-17","open":46.415,"close":46.255,"high":46.415,"low":46.145,"turnoverPieces":314,"turnoverEuro":14539.8},{"date":"2020-07-16","open":46.7,"close":46.395,"high":46.905,"low":46.395,"turnoverPieces":4347,"turnoverEuro":203755.75},{"date":"2020-07-15","open":46.785,"close":46.825,"high":47.065,"low":46.67,"turnoverPieces":279,"turnoverEuro":13092.23},{"date":"2020-07-14","open":46.405,"close":46.435,"high":46.585,"low":46.155,"turnoverPieces":873,"turnoverEuro":40321.05},{"date":"2020-07-13","open":46.94,"close":47,"high":47.15,"low":46.94,"turnoverPieces":1848,"turnoverEuro":86908.45},{"date":"2020-07-10","open":46.62,"close":46.67,"high":46.73,"low":46.555,"turnoverPieces":199,"turnoverEuro":9283.76},{"date":"2020-07-09","open":47.05,"close":46.435,"high":47.05,"low":46.37,"turnoverPieces":2031,"turnoverEuro":95043.24},{"date":"2020-07-08","open":47.105,"close":46.98,"high":47.32,"low":46.98,"turnoverPieces":447,"turnoverEuro":21099.96},{"date":"2020-07-07","open":47.94,"close":47.57,"high":47.94,"low":47.4,"turnoverPieces":2613,"turnoverEuro":124366.22},{"date":"2020-07-06","open":48.56,"close":48.525,"high":49.065,"low":48.43,"turnoverPieces":404,"turnoverEuro":19616.16},{"date":"2020-07-03","open":48.305,"close":48.22,"high":48.465,"low":48.22,"turnoverPieces":1177,"turnoverEuro":56929.38},{"date":"2020-07-02","open":48.27,"close":48.475,"high":48.825,"low":48.165,"turnoverPieces":3188,"turnoverEuro":154996.02},{"date":"2020-07-01","open":47.735,"close":47.78,"high":47.895,"low":47.2,"turnoverPieces":3121,"turnoverEuro":147392.48},{"date":"2020-06-30","open":47.07,"close":47.345,"high":47.565,"low":46.775,"turnoverPieces":1646,"turnoverEuro":77800.72},{"date":"2020-06-29","open":46.6,"close":46.895,"high":47.055,"low":46.545,"turnoverPieces":15304,"turnoverEuro":712461.42},{"date":"2020-06-26","open":47.36,"close":47.105,"high":47.63,"low":47.105,"turnoverPieces":4875,"turnoverEuro":231649.97},{"date":"2020-06-25","open":46.835,"close":46.715,"high":47.105,"low":46.705,"turnoverPieces":3140,"turnoverEuro":147065.83},{"date":"2020-06-24","open":48.605,"close":46.445,"high":48.605,"low":46.445,"turnoverPieces":6442,"turnoverEuro":306206.32},{"date":"2020-06-23","open":48.06,"close":48.16,"high":48.325,"low":48.035,"turnoverPieces":15437,"turnoverEuro":744003.53},{"date":"2020-06-22","open":48.24,"close":47.86,"high":48.345,"low":47.86,"turnoverPieces":2356,"turnoverEuro":113269.84},{"date":"2020-06-19","open":49.765,"close":49.385,"high":50.25,"low":49.075,"turnoverPieces":13941,"turnoverEuro":690995.6},{"date":"2020-06-18","open":49.62,"close":49.38,"high":49.62,"low":48.925,"turnoverPieces":6638,"turnoverEuro":326831.93},{"date":"2020-06-17","open":49.83,"close":50.26,"high":50.45,"low":49.78,"turnoverPieces":8054,"turnoverEuro":404616.53},{"date":"2020-06-16","open":49.035,"close":50.14,"high":50.48,"low":49.035,"turnoverPieces":3357,"turnoverEuro":168771.76},{"date":"2020-06-15","open":47.92,"close":48.245,"high":48.66,"low":46.985,"turnoverPieces":1244,"turnoverEuro":59710.44},{"date":"2020-06-12","open":47.28,"close":48.145,"high":48.645,"low":47.28,"turnoverPieces":1737,"turnoverEuro":84150.62},{"date":"2020-06-11","open":48.93,"close":47.265,"high":48.93,"low":47.12,"turnoverPieces":599,"turnoverEuro":28787.68},{"date":"2020-06-10","open":51.21,"close":50.16,"high":51.21,"low":50.09,"turnoverPieces":6308,"turnoverEuro":318983},{"date":"2020-06-09","open":52.02,"close":50.84,"high":52.02,"low":50.84,"turnoverPieces":1745,"turnoverEuro":89448.13},{"date":"2020-06-08","open":50.83,"close":51.78,"high":51.85,"low":50.83,"turnoverPieces":2861,"turnoverEuro":147960.48},{"date":"2020-06-05","open":48.675,"close":51.3,"high":51.64,"low":48.675,"turnoverPieces":1110,"turnoverEuro":57092.74},{"date":"2020-06-04","open":49.24,"close":48.955,"high":49.3,"low":48.955,"turnoverPieces":612,"turnoverEuro":30051.33},{"date":"2020-06-03","open":49.42,"close":49.545,"high":49.64,"low":48.53,"turnoverPieces":231,"turnoverEuro":11456.38},{"date":"2020-06-02","open":47.77,"close":48.19,"high":48.19,"low":47.64,"turnoverPieces":128,"turnoverEuro":6117.93},{"date":"2020-05-29","open":47.225,"close":46.52,"high":47.39,"low":46.52,"turnoverPieces":2651,"turnoverEuro":124903.08},{"date":"2020-05-28","open":47.42,"close":47.145,"high":47.9,"low":47.145,"turnoverPieces":1203,"turnoverEuro":57292.82},{"date":"2020-05-27","open":47.25,"close":47.185,"high":47.67,"low":47.185,"turnoverPieces":1690,"turnoverEuro":79905.72},{"date":"2020-05-26","open":45.99,"close":46.72,"high":46.72,"low":45.99,"turnoverPieces":664,"turnoverEuro":30861.74},{"date":"2020-05-25","open":45.62,"close":45.855,"high":46.01,"low":45.49,"turnoverPieces":795,"turnoverEuro":36426.19},{"date":"2020-05-22","open":45.05,"close":45.045,"high":45.23,"low":45.045,"turnoverPieces":27,"turnoverEuro":1221.21},{"date":"2020-05-21","open":44.935,"close":45.095,"high":45.25,"low":44.935,"turnoverPieces":1189,"turnoverEuro":53628.97},{"date":"2020-05-20","open":45.11,"close":45.025,"high":45.245,"low":44.915,"turnoverPieces":789,"turnoverEuro":35481.99},{"date":"2020-05-19","open":45.47,"close":45.19,"high":45.47,"low":44.91,"turnoverPieces":1151,"turnoverEuro":51795.57},{"date":"2020-05-18","open":43.81,"close":45.29,"high":45.29,"low":43.81,"turnoverPieces":1971,"turnoverEuro":86579.79},{"date":"2020-05-15","open":43.81,"close":43.205,"high":43.81,"low":42.995,"turnoverPieces":487,"turnoverEuro":21252.67},{"date":"2020-05-14","open":43.425,"close":42.71,"high":43.665,"low":42.71,"turnoverPieces":4925,"turnoverEuro":211295.9},{"date":"2020-05-13","open":44.605,"close":43.885,"high":44.63,"low":43.885,"turnoverPieces":2802,"turnoverEuro":124344.25},{"date":"2020-05-12","open":46.145,"close":45.205,"high":46.325,"low":45.205,"turnoverPieces":960,"turnoverEuro":44365.08},{"date":"2020-05-11","open":47.325,"close":46.83,"high":47.325,"low":46.68,"turnoverPieces":1714,"turnoverEuro":80431.72},{"date":"2020-05-08","open":46.7,"close":46.77,"high":46.975,"low":46.565,"turnoverPieces":2858,"turnoverEuro":133400.7},{"date":"2020-05-07","open":45.6,"close":46.37,"high":46.37,"low":45.6,"turnoverPieces":4470,"turnoverEuro":205582.01},{"date":"2020-05-06","open":46.1,"close":45.655,"high":46.295,"low":45.655,"turnoverPieces":4975,"turnoverEuro":228929.62},{"date":"2020-05-05","open":45.84,"close":46.37,"high":46.37,"low":45.835,"turnoverPieces":1871,"turnoverEuro":85900.31},{"date":"2020-05-04","open":45.675,"close":45.015,"high":46.4,"low":44.9,"turnoverPieces":4608,"turnoverEuro":209044.1},{"date":"2020-04-30","open":47.875,"close":46.8,"high":48.07,"low":46.8,"turnoverPieces":5190,"turnoverEuro":248746.64},{"date":"2020-04-29","open":47.165,"close":47.955,"high":48.07,"low":47.165,"turnoverPieces":1558,"turnoverEuro":73932},{"date":"2020-04-28","open":46.26,"close":47.085,"high":47.125,"low":46.26,"turnoverPieces":1672,"turnoverEuro":77897.29},{"date":"2020-04-27","open":45.49,"close":45.925,"high":46.495,"low":45.45,"turnoverPieces":4037,"turnoverEuro":183889.38},{"date":"2020-04-24","open":45.425,"close":44.82,"high":45.425,"low":44.82,"turnoverPieces":0,"turnoverEuro":0},{"date":"2020-04-23","open":45.095,"close":45.165,"high":45.41,"low":45.095,"turnoverPieces":812,"turnoverEuro":36828.5},{"date":"2020-04-22","open":44.615,"close":44.885,"high":44.885,"low":44.615,"turnoverPieces":680,"turnoverEuro":30445.13},{"date":"2020-04-21","open":45.375,"close":44.245,"high":45.375,"low":44.24,"turnoverPieces":19075,"turnoverEuro":851619.02},{"date":"2020-04-20","open":46.835,"close":45.96,"high":46.835,"low":45.86,"turnoverPieces":4257,"turnoverEuro":197128.52},{"date":"2020-04-17","open":46.165,"close":46.54,"high":47,"low":46.165,"turnoverPieces":1448,"turnoverEuro":67351.37},{"date":"2020-04-16","open":45.965,"close":45.35,"high":46.145,"low":45.35,"turnoverPieces":1531,"turnoverEuro":70046.35},{"date":"2020-04-15","open":47.525,"close":46.045,"high":47.525,"low":45.65,"turnoverPieces":11828,"turnoverEuro":543149.86},{"date":"2020-04-14","open":47,"close":47.18,"high":48.07,"low":46.555,"turnoverPieces":4001,"turnoverEuro":190176.11},{"date":"2020-04-09","open":46.255,"close":48.065,"high":48.47,"low":46.19,"turnoverPieces":1378,"turnoverEuro":63926.92},{"date":"2020-04-08","open":44.62,"close":45.29,"high":45.29,"low":43.93,"turnoverPieces":5340,"turnoverEuro":238757.24},{"date":"2020-04-07","open":44.01,"close":44.955,"high":45.74,"low":44.01,"turnoverPieces":5319,"turnoverEuro":239725.21},{"date":"2020-04-06","open":42.01,"close":43.1,"high":43.48,"low":42.01,"turnoverPieces":4589,"turnoverEuro":195844.51}],"totalCount":167,"tradedInPercent":false}        """.trimIndent()

        val history = Json.decodeFromString(
            History.serializer(), json
        )
    }

     */
}