package io.antmedia.plugin;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.plugin.api.IStreamListener;
import io.antmedia.rest.ResponsePair;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.vertx.core.Vertx;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component(value="plugin.webpageRecordingPlugin")
public class WebpageRecordingPlugin implements ApplicationContextAware, IStreamListener{

	public static final String BEAN_NAME = "web.handler";
	protected static Logger logger = LoggerFactory.getLogger(WebpageRecordingPlugin.class);
	private final String EXTENSION_ID = "anoaibdoojapjdknicdngigmlijaanik";

	private HashMap<String, WebDriver> drivers = new HashMap<String, WebDriver>();

	private Vertx vertx;
	private ApplicationContext applicationContext;

	public HashMap<String, WebDriver> getDrivers() {
		return drivers;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
		vertx = (Vertx) applicationContext.getBean("vertxCore");

		AntMediaApplicationAdapter app = getApplication();
		app.addStreamListener(this);
	}

	public ResponsePair startWebpageRecording(String streamId, String websocketUrl, String url) throws URISyntaxException, InterruptedException {
		ResponsePair responsePair = new ResponsePair();
		if (getDrivers().containsKey(streamId)) {
			logger.warn("Driver already exists for stream id: {}", streamId);
			responsePair.setResponseCode(ResponsePair.INTERNAL_SERVER_ERROR_CODE);
			responsePair.setResponse("Driver already exists for stream id: " + streamId);
			return responsePair;
		}

		WebDriver driver = createDriver();
		if (driver == null) {
			logger.error("Driver cannot created");
			responsePair.setResponseCode(ResponsePair.INTERNAL_SERVER_ERROR_CODE);
			responsePair.setResponse("Driver cannot created");
			return responsePair;
		}
		drivers.put(streamId, driver);
		driver.get(url);
		TimeUnit.SECONDS.sleep(5);
		customModification(driver);
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript(String.format("window.postMessage({ command:  'WR_START_BROADCASTING', streamId: '%s', websocketURL: '%s' }, '*')", streamId, websocketUrl));
		responsePair.setResponse("Webpage recording started");
		responsePair.setResponseCode(ResponsePair.SUCCESS_CODE);
		return responsePair;
	}

	public void customModification(WebDriver driver) {
		// you add related selenium code here to play the video on a custom page or login to a page

		/* example code to start YouTube video
		new Actions(driver)
				.sendKeys("k")
				.perform();
		 */
		Wait<WebDriver> wait = new FluentWait<WebDriver>(driver)
				.withTimeout(Duration.ofSeconds(30))
				.pollingEvery(Duration.ofSeconds(5))
				.ignoring(NoSuchElementException.class);

		WebElement foo = wait.until(new Function<WebDriver, WebElement>() {
										public WebElement apply(WebDriver driver) {
											return driver.findElement(By.id("participant_name"));
										}
									});

		driver.findElement(By.id("participant_name")).sendKeys("test");
		driver.findElement(By.id("room_join_button")).click();
	}

	public ResponsePair stopWebpageRecording(String streamId) throws InterruptedException {
		ResponsePair responsePair = new ResponsePair();
		if (!drivers.containsKey(streamId)) {
			logger.warn("Driver does not exist for stream id: {}", streamId);
			responsePair.setResponseCode(ResponsePair.INTERNAL_SERVER_ERROR_CODE);
			responsePair.setResponse("Driver does not exist for stream id: " + streamId);
			return responsePair;
		}

		WebDriver driver = drivers.get(streamId);
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript(String.format("window.postMessage({ command:  'STOP', streamId: '%s' }, '*')", streamId));
		TimeUnit.SECONDS.sleep(5);
		driver.quit();
		drivers.remove(streamId);
		responsePair.setResponse("Webpage recording stopped");
		responsePair.setResponseCode(ResponsePair.SUCCESS_CODE);
		return responsePair;
	}

	public WebDriver createDriver() throws URISyntaxException {
		WebDriverManager.chromedriver().setup();
		ChromeOptions options = new ChromeOptions();
		List<String> args = new ArrayList<>();

		args.add("--enable-usermedia-screen-capturing");
		args.add("--allow-http-screen-capture");
		args.add("--disable-infobars");
		args.add("--enable-tab-capture");
		args.add("--no-sandbox");
		args.add(String.format("--whitelisted-extension-id=%s", EXTENSION_ID));
		args.add("--headless=new");
		try {
			options.addExtensions(getExtensionFileFromResource());
		} catch (IOException e) {
			logger.error(e.getMessage());
			return null;
		}
		options.addArguments(args);

		return new ChromeDriver(options);
	}

	private File getExtensionFileFromResource() throws URISyntaxException, IOException {

		ClassLoader classLoader = getClass().getClassLoader();
		InputStream inputStream = classLoader.getResourceAsStream("webpage-recording-extension.crx");
		if (inputStream == null) {
			throw new IllegalArgumentException("webpage-recording-extension not found!");
		} else {
			File targetFile = new File("src/main/resources/targetFile.tmp");
			FileUtils.copyInputStreamToFile(inputStream, targetFile);
			return targetFile;
		}

	}
	
	public AntMediaApplicationAdapter getApplication() {
		return (AntMediaApplicationAdapter) applicationContext.getBean(AntMediaApplicationAdapter.BEAN_NAME);
	}

	@Override
	public void streamStarted(String streamId) {
		System.out.println("***************");
		System.out.println("Stream Started:"+streamId);
		System.out.println("***************");
	}

	@Override
	public void streamFinished(String streamId) {
		System.out.println("***************");
		System.out.println("Stream Finished:"+streamId);
		System.out.println("***************");
	}

	@Override
	public void joinedTheRoom(String roomId, String streamId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void leftTheRoom(String roomId, String streamId) {
		// TODO Auto-generated method stub
		
	}

}
