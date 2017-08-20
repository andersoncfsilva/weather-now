package br.com.andesoncfsilva.weathernow.di.modules

import android.content.Context
import br.com.andesoncfsilva.weathernow.BuildConfig
import br.com.andesoncfsilva.weathernow.WeatherNowApplication
import br.com.andesoncfsilva.weathernow.data.*
import br.com.andesoncfsilva.weathernow.di.executors.JobExecutor
import br.com.andesoncfsilva.weathernow.di.executors.PostExecutionThread
import br.com.andesoncfsilva.weathernow.di.executors.ThreadExecutor
import br.com.andesoncfsilva.weathernow.di.executors.UIThread
import br.com.andesoncfsilva.weathernow.utils.HardwareUtil
import br.com.andesoncfsilva.weathernow.utils.HardwareUtilImpl
import br.com.andesoncfsilva.weathernow.utils.OpenWeatherMapUtil
import br.com.andesoncfsilva.weathernow.utils.OpenWeatherMapUtilImpl
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Created by Anderson Silva on 16/08/17.
 *
 */
@Module class AppModule(val app: WeatherNowApplication) {

    @Provides @Singleton fun context(): Context = app

    @Provides @Singleton fun provideThreadExecutor(jobExecutor: JobExecutor): ThreadExecutor = jobExecutor

    @Provides @Singleton fun providePostExecutionThread(uiThread: UIThread): PostExecutionThread = uiThread

    @Provides @Singleton fun provideGson(): Gson {
        val gsonBuilder = GsonBuilder()
        return gsonBuilder.create()
    }

    @Provides @Singleton fun provideOkhttpClient(context: Context): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        val timeout: Long = 5
        val client = OkHttpClient.Builder()

        //setup cache
        val httpCacheDirectory = File(context.cacheDir, "responses")
        val cacheSize = 10 * 1024 * 1024 // 10 MiB
        val cache = Cache(httpCacheDirectory, cacheSize.toLong())

//add cache to the client
        client.cache(cache)
        client.networkInterceptors().add(logging)
        client.connectTimeout(timeout, TimeUnit.MINUTES)
        client.writeTimeout(timeout, TimeUnit.MINUTES)
        client.readTimeout(timeout, TimeUnit.MINUTES)

        return client.build()
    }


    @Provides @Singleton fun provideRetrofit(gson: Gson, okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl(BuildConfig.OPEN_WEATHER_MAP_URL)
                .client(okHttpClient).build()
    }

    @Provides @Singleton fun provideRestAPI(retrofit: Retrofit): RestApi = retrofit.create(RestApi::class.java)

    @Provides @Singleton fun provideHardwareUtil(hardwareUtil: HardwareUtilImpl): HardwareUtil = hardwareUtil

    @Provides @Singleton fun provideOpenWeatherMapApiKey(): OpenWeatherMapUtil = OpenWeatherMapUtilImpl()

    @Provides @Singleton fun provideWeatherApi(weatherApi: WeatherApiImpl): WeatherApi = weatherApi

    @Provides @Singleton fun provideMapperCityWeather(mapperCityWeather: MapperCityWeatherImpl): MapperCityWeather = mapperCityWeather

}
