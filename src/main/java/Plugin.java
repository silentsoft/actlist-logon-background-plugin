import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import javax.imageio.ImageIO;

import org.apache.commons.net.util.Base64;
import org.silentsoft.actlist.plugin.ActlistPlugin;
import org.silentsoft.core.util.SystemUtil;
import org.silentsoft.core.util.SystemUtil.RegType;


public class Plugin extends ActlistPlugin {

	@FXML
	private ListView<ImageView> imagePreview;
	
	@FXML
	private Button customButton;
	
	public static void main(String args[]) throws Exception {
		debug();
	}
	
	public Plugin() {
		super("Custom Logon Background");
		
		setPluginVersion("1.0.1");
		setPluginAuthor("silentsoft.org", URI.create("https://github.com/silentsoft/actlist-plugin-logon-background"));
		setPluginUpdateCheckURI(URI.create("http://actlist.silentsoft.org/api/plugin/47878d31/update/check"));
		setPluginDescription("Custom Logon Background for Windows 7");
		
		setMinimumCompatibleVersion(1, 2, 6);
	}
	
	@Override
	protected void initialize() throws Exception {
		imagePreview.setPlaceholder(new Label("Drag and Drop image file to here."));
		
		imagePreview.setOnDragEntered(dragEvent -> {
			imagePreview.getStyleClass().clear();
			imagePreview.getStyleClass().add("view-accent");
		});
		
		imagePreview.setOnDragOver(dragEvent -> {
			Dragboard dragboard = dragEvent.getDragboard();
			if (dragboard.hasFiles()) {
				List<File> files = dragboard.getFiles();
				if (files.size() == 1) {
					File file = files.get(0);
					
					boolean isValidImageFile = true;
					try {
					    BufferedImage image = ImageIO.read(file);
					    if (image == null) {
					        isValidImageFile = false;
					    }
					} catch(IOException ex) {
					    isValidImageFile = false;
					}
					
					if (isValidImageFile) {
						dragEvent.acceptTransferModes(TransferMode.LINK);
						dragEvent.consume(); // TODO Do I need to consume the event on this case ?
					} else {
						dragEvent.consume();
					}
				} else {
					dragEvent.consume();
				}
			} else {
				dragEvent.consume();
			}
		});
		
		imagePreview.setOnDragDropped(dragEvent -> {
			try {
				File imageFile = dragEvent.getDragboard().getFiles().get(0);
				
				ImageView imageView = new ImageView(imageFile.toURI().toURL().toExternalForm());
				imageView.fitWidthProperty().bind(imagePreview.widthProperty().subtract(16));   // subtract size of scroll
				imageView.fitHeightProperty().bind(imagePreview.heightProperty().subtract(8));  // subtract size of scroll
				
				imagePreview.getItems().clear();
				imagePreview.getItems().add(imageView);
				
				Image image = imageView.getImage();
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				ImageIO.write(SwingFXUtils.fromFXImage(image, null), "jpg", byteArrayOutputStream);
				
				String base64 = new String(Base64.encodeBase64(byteArrayOutputStream.toByteArray()), "UTF-8");
				putConfig("backgroundDefault.jpg", base64);
				
				customButton.setDisable(false);
				
				imagePreview.getStyleClass().clear();
				imagePreview.getStyleClass().add("view-image");
				
				byteArrayOutputStream.close();
			} catch (Exception e) {
				throwException(e);
			}
		});
		
		imagePreview.setOnDragExited(dragEvent -> {
			if (imagePreview.getItems().size() == 0) {
				imagePreview.getStyleClass().clear();
				imagePreview.getStyleClass().add("view-default");
			}
		});
	}

	@Override
	public void pluginActivated() throws Exception {
		String base64 = getConfig("backgroundDefault.jpg");
		
		if (base64 == null || "".equals(base64)) {
			customButton.setDisable(true);
		} else {
			customButton.setDisable(false);
			
			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(Base64.decodeBase64(base64));
			
			ImageView imageView = new ImageView(new Image(byteArrayInputStream));
			imageView.fitWidthProperty().bind(imagePreview.widthProperty().subtract(16));   // subtract size of scroll
			imageView.fitHeightProperty().bind(imagePreview.heightProperty().subtract(8));  // subtract size of scroll
			
			imagePreview.getItems().clear();
			imagePreview.getItems().add(imageView);
			
			imagePreview.getStyleClass().clear();
			imagePreview.getStyleClass().add("view-image");
			
			byteArrayInputStream.close();
		}
	}

	@Override
	public void pluginDeactivated() throws Exception {
		imagePreview.getItems().clear();
	}

	@FXML
	private void setDefaultLogonBackground() {
		try {
			SystemUtil.writeRegistry("HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Authentication\\LogonUI\\Background", "OEMBackground", RegType.REG_DWORD, 0);
		} catch (Exception e) {
			throwException(e);
		}
	}
	
	@FXML
	private void setCustomLogonBackground() {
		try {
			String base64 = getConfig("backgroundDefault.jpg");
			if (base64 == null || "".equals(base64)) {
				return; // defense logic to prevent weird exception. it could be situation that the user deleted a config file.
			}
			
			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(Base64.decodeBase64(base64));
			BufferedImage image = ImageIO.read(byteArrayInputStream);
			byteArrayInputStream.close();
			
			File file = new File("C:\\Windows\\System32\\oobe\\info\\backgrounds\\backgroundDefault.jpg");
			if (file.getParentFile().exists() == false) {
				file.getParentFile().mkdirs();
			}
			
			ImageIO.write(image, "jpg", file);
			
			SystemUtil.writeRegistry("HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Authentication\\LogonUI\\Background", "OEMBackground", RegType.REG_DWORD, 1);
		} catch (Exception e) {
			throwException(e);
		}
	}
	
}
