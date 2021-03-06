package eu.depau.etchdroid.libdepaums.fs.fat32;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import eu.depau.etchdroid.libdepaums.driver.BlockDeviceDriver;
import eu.depau.etchdroid.libdepaums.driver.ByteBlockDevice;
import eu.depau.etchdroid.libdepaums.driver.file.FileBlockDeviceDriver;
import eu.depau.etchdroid.libdepaums.util.Pair;
import org.apache.commons.io.IOUtils;
import org.junit.runner.RunWith;
import org.xenei.junit.contract.Contract;
import org.xenei.junit.contract.ContractImpl;
import org.xenei.junit.contract.ContractSuite;
import org.xenei.junit.contract.IProducer;

import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.fail;

/**
 * Created by magnusja on 02/08/17.
 */
@RunWith(ContractSuite.class)
@ContractImpl(Fat32FileSystem.class)
public class Fat32FileSystemTest {

	IProducer<Pair<Fat32FileSystem, JsonObject>> producer =
			new IProducer<Pair<Fat32FileSystem, JsonObject>>() {

				private BlockDeviceDriver blockDevice;
				private FileBlockDeviceDriver fileBlockDevice;

				public Pair<Fat32FileSystem, JsonObject> newInstance() {
					try {
						JsonObject obj = Json.parse(IOUtils.toString(
								new URL("https://www.dropbox.com/s/nek7bu08prykkhv/expectedValues.json?dl=1")
										.openStream())).asObject();

						if (blockDevice == null) {
							fileBlockDevice = new FileBlockDeviceDriver(
									new URL("https://www.dropbox.com/s/3bxngiqmwitlucd/mbr_fat32.img?dl=1"),
									obj.get("blockSize").asInt(),
									obj.get("blockSize").asInt() * obj.get("fileSystemOffset").asInt());
							blockDevice = new ByteBlockDevice(fileBlockDevice);
							blockDevice.init();
						}
						return new Pair<>(Fat32FileSystem.read(blockDevice), obj);
					} catch (IOException e) {
						e.printStackTrace();
						fail();
					}

					return null;
				}

				public void cleanUp() {
				}
			};

	@Contract.Inject
	public IProducer<Pair<Fat32FileSystem, JsonObject>> makeFat32FileSystem() {
		return producer;
	}
}