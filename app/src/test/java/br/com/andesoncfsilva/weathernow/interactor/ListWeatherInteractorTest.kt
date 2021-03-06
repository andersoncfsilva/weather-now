package br.com.andesoncfsilva.weathernow.interactor

import br.com.andesoncfsilva.weathernow.data.MapperCityWeather
import br.com.andesoncfsilva.weathernow.data.WeatherApi
import br.com.andesoncfsilva.weathernow.di.executors.PostExecutionThread
import br.com.andesoncfsilva.weathernow.di.executors.ThreadExecutor
import br.com.andesoncfsilva.weathernow.entities.ListCitiesWeather
import br.com.andesoncfsilva.weathernow.exception.NoConnectionException
import br.com.andesoncfsilva.weathernow.exception.RestAPIException
import br.com.andesoncfsilva.weathernow.interactors.ListWeatherInteractor
import br.com.andesoncfsilva.weathernow.interactors.ListWeatherInteractorImpl
import br.com.andesoncfsilva.weathernow.util.MockHelper
import br.com.andesoncfsilva.weathernow.utils.GeoCalculator
import br.com.andesoncfsilva.weathernow.utils.HardwareUtil
import io.reactivex.Observable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * Created by Anderson Silva on 20/08/17.
 *
 */
class ListWeatherInteractorTest {


    @Mock lateinit var mockPostExecutionThread: PostExecutionThread
    @Mock lateinit var mockThreadExecutor: ThreadExecutor
    @Mock lateinit var mockHardwareUtil: HardwareUtil
    @Mock lateinit var mockWeatherApi: WeatherApi
    @Mock lateinit var mockGeoCalculator: GeoCalculator
    @Mock lateinit var mockMapperCityWeather: MapperCityWeather

    lateinit var interactor: ListWeatherInteractor

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        setupMockReturns()

        interactor = ListWeatherInteractorImpl(
                mockThreadExecutor,
                mockHardwareUtil,
                mockPostExecutionThread,
                mockWeatherApi,
                mockMapperCityWeather,
                mockGeoCalculator)
    }

    private fun setupMockReturns() {
        `when`(mockPostExecutionThread.scheduler).thenReturn(Schedulers.trampoline())
        `when`(mockThreadExecutor.scheduler).thenReturn(Schedulers.trampoline())
        `when`(mockMapperCityWeather.convert(MockHelper.currentWeatherResponse, MockHelper.unitTemp)).thenReturn(MockHelper.citiesWeather)
        `when`(mockHardwareUtil.connected()).thenReturn(true)
        `when`(mockGeoCalculator.calculateBox(MockHelper.latitude, MockHelper.longitude)).thenReturn(MockHelper.geoBox)
        `when`(mockGeoCalculator.calculateDistance(MockHelper.cityWeather, MockHelper.latitude, MockHelper.longitude)).thenReturn(50000.0)
        `when`(mockWeatherApi.getCurrentWeather(any(), any(), any(), any(), any())).thenReturn(Observable.just(MockHelper.currentWeatherResponse))
    }

    @Test
    fun shouldExecuteInteractorWithoutErrorsInsideRange() {
        var executeOk = false
        var result: ListCitiesWeather? = null

        interactor.execute(
                MockHelper.unitTemp,
                MockHelper.latitude,
                MockHelper.longitude,
                MockHelper.latitude,
                MockHelper.longitude,
                Consumer { executeOk = true; result = it },
                Consumer { fail("error", it) })

        assertThat(executeOk).isTrue()
        assertThat(result).isNotNull()
        assertThat(result?.cities?.count()).isEqualTo(MockHelper.currentWeatherResponse.city?.count())

        verify(mockMapperCityWeather).convert(MockHelper.currentWeatherResponse, MockHelper.unitTemp)
        verify(mockHardwareUtil).connected()
        verify(mockGeoCalculator).calculateBox(MockHelper.latitude, MockHelper.longitude)
        verify(mockGeoCalculator, times(MockHelper.citiesWeather.count() * 2)).calculateDistance(MockHelper.cityWeather, MockHelper.latitude, MockHelper.longitude)
        verify(mockWeatherApi).getCurrentWeather(any(), any(), any(), any(), any())

    }

    @Test
    fun shouldExecuteInteractorWithoutErrorsOutsideRange() {
        var executeOk = false
        var result: ListCitiesWeather? = null

        `when`(mockGeoCalculator.calculateDistance(MockHelper.cityWeather, MockHelper.latitude, MockHelper.longitude)).thenReturn(51000.0)

        interactor.execute(
                MockHelper.unitTemp,
                MockHelper.latitude,
                MockHelper.longitude,
                MockHelper.latitude,
                MockHelper.longitude,
                Consumer { executeOk = true; result = it },
                Consumer { fail("error", it) })

        assertThat(executeOk).isTrue()
        assertThat(result).isNotNull()
        assertThat(result?.cities?.count()).isEqualTo(0)

        verify(mockMapperCityWeather).convert(MockHelper.currentWeatherResponse, MockHelper.unitTemp)
        verify(mockHardwareUtil).connected()
        verify(mockGeoCalculator).calculateBox(MockHelper.latitude, MockHelper.longitude)
        verify(mockGeoCalculator, times(MockHelper.citiesWeather.count() * 2)).calculateDistance(MockHelper.cityWeather, MockHelper.latitude, MockHelper.longitude)
        verify(mockWeatherApi).getCurrentWeather(any(), any(), any(), any(), any())

    }

    @Test
    fun shouldThrowsNoConnectionException() {
        var executeOk = false
        var result: ListCitiesWeather? = null
        var error: Throwable? = null

        `when`(mockHardwareUtil.connected()).thenReturn(false)

        interactor.execute(
                MockHelper.unitTemp,
                MockHelper.latitude,
                MockHelper.longitude,
                MockHelper.latitude,
                MockHelper.longitude,
                Consumer { executeOk = true; result = it },
                Consumer { error = it })

        assertThat(executeOk).isFalse()
        assertThat(result).isNull()
        assertThat(error).isInstanceOf(NoConnectionException::class.java)

        verify(mockHardwareUtil).connected()

    }

    @Test
    fun shouldThrowsRestAPIException() {
        var executeOk = false
        var result: ListCitiesWeather? = null
        var error: Throwable? = null

        `when`(mockWeatherApi.getCurrentWeather(any(), any(), any(), any(), any())).thenReturn(Observable.error(RestAPIException(RuntimeException())))

        interactor.execute(
                MockHelper.unitTemp,
                MockHelper.latitude,
                MockHelper.longitude,
                MockHelper.latitude,
                MockHelper.longitude,
                Consumer { executeOk = true; result = it },
                Consumer { error = it })

        assertThat(executeOk).isFalse()
        assertThat(result).isNull()
        assertThat(error).isInstanceOf(RestAPIException::class.java)

        verify(mockHardwareUtil).connected()
        verify(mockGeoCalculator).calculateBox(MockHelper.latitude, MockHelper.longitude)
        verify(mockWeatherApi).getCurrentWeather(any(), any(), any(), any(), any())

    }
}