package si.um.feri.projketRRI;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.net.HttpParametersUtils;
import com.badlogic.gdx.net.HttpStatus;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.HashMap;
import java.util.Map;

public class LoginScreen extends ScreenAdapter {

    private static final String API_URL = "http://localhost:3000"; //"https://pi.darkosever.si";
    private static final String TOKEN_PREFS = "auth";
    private static final String TOKEN_KEY = "token";

    private final Stage stage;
    private final Skin skin;

    private TextField usernameField;
    private TextField passwordField;
    private Label statusLabel;
    private TextButton loginButton;

    private final Projekt projekt;
    private Table root;
    private boolean built;
    private Texture logoTexture;

    public LoginScreen(Projekt projekt) {
        Gdx.app.log("LoginScreen", "CONSTRUCTOR " + this);
        this.projekt = projekt;
        this.skin = projekt.skin;
        this.stage = new Stage(new ScreenViewport());
        logoTexture = new Texture(Gdx.files.internal("images/cebelar.png"));
    }

    @Override
    public void show() {

        Gdx.app.log("LoginScreen", "show()");

        stage.clear();
        Gdx.input.setInputProcessor(stage);

        if (!built) {
            buildUi();
            built = true;
        }

        String token = Gdx.app.getPreferences("auth").getString("token", "");
        if (!token.isEmpty()) {
            projekt.setScreen(new RasterMapScreen(projekt));
            dispose();
        }

    }

    private void buildUi() {

        Image logoImage = new Image(logoTexture);
        logoImage.setScaling(Scaling.fit);


        root = new Table();
        root.setFillParent(true);
        root.pad(40);
        stage.addActor(root);

        Table root = new Table(skin);
        root.setFillParent(true);
        root.pad(28);
        root.setBackground("default-round");
        stage.addActor(root);

        Table panel = new Table(skin);
        panel.setBackground("default-round");
        panel.pad(30);

        usernameField = new TextField("", skin);
        usernameField.setMessageText("Username");

        passwordField = new TextField("", skin);
        passwordField.setMessageText("Password");
        passwordField.setPasswordMode(true);
        passwordField.setPasswordCharacter('*');

        loginButton = new TextButton("Login", skin);

        statusLabel = new Label("", skin);
        statusLabel.setAlignment(Align.center);

        root.center();
        root.add(logoImage)
            .width(Value.percentWidth(0.3f, root))
            .height(Value.percentWidth(0.3f, root))
            .colspan(2)
            .padBottom(20)
            .center()
            .row();
        root.add(new Label("Username:", skin)).left().padRight(15);
        root.add(usernameField).growX().row();
        root.add(new Label("Password:", skin)).left().padRight(15).padTop(10);
        root.add(passwordField).growX().padTop(10).row();
        loginButton.setColor(0.2f, 0.6f, 1f, 1f);
        root.add(loginButton).colspan(2).padTop(20).width(500).row();
        root.add(statusLabel).colspan(2).padTop(10).width(500).row();

        loginButton.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                doLogin();
            }
        });
    }

    private void doLogin() {
        final String username = usernameField.getText().trim();
        final String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Vnesi username in password.");
            return;
        }

        setUiEnabled(false);
        statusLabel.setText("Posiljam...");

        String url = API_URL + "/users/login";

        // Body kot JSON:
        // {"username":"...","password":"..."}
        String jsonBody = "{\"username\":\"" + escapeJson(username) + "\",\"password\":\"" + escapeJson(password) + "\"}";

        Net.HttpRequest req = new HttpRequestBuilder()
                .newRequest()
                .method(Net.HttpMethods.POST)
                .url(url)
                .header("Content-Type", "application/json")
                .content(jsonBody)
                .build();

        Gdx.net.sendHttpRequest(req, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int status = httpResponse.getStatus().getStatusCode();
                String body = httpResponse.getResultAsString();

                // UI update mora v render thread:
                Gdx.app.postRunnable(() -> {
                    setUiEnabled(true);

                    if (status >= 200 && status < 300) {
                        try {
                            JsonValue root = new JsonReader().parse(body);
                            String token = root.getString("token", null);

                            if (token == null || token.isEmpty()) {
                                statusLabel.setText("Login OK, ampak ni tokena v odzivu.");
                                return;
                            }

                            saveToken(token);
                            statusLabel.setText("Login uspešen! Token shranjen.");

                            projekt.setScreen(new RasterMapScreen(projekt));
                            dispose();

                        } catch (Exception e) {
                            statusLabel.setText("Napaka pri branju JSON: " + e.getMessage());
                        }
                    } else {
                        statusLabel.setText("Login ni uspel (" + status + "): " + body);
                    }
                });
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.postRunnable(() -> {
                    setUiEnabled(true);
                    statusLabel.setText("Request failed: " + t.getMessage());
                });
            }

            @Override
            public void cancelled() {
                Gdx.app.postRunnable(() -> {
                    setUiEnabled(true);
                    statusLabel.setText("Preklicano.");
                });
            }
        });
    }

    private void setUiEnabled(boolean enabled) {
        usernameField.setDisabled(!enabled);
        passwordField.setDisabled(!enabled);
        loginButton.setDisabled(!enabled);
    }

    private void saveToken(String token) {
        Preferences prefs = Gdx.app.getPreferences(TOKEN_PREFS);
        prefs.putString(TOKEN_KEY, token);
        prefs.flush(); // pomembno: zapiše na disk
    }

    private String getToken() {
        Preferences prefs = Gdx.app.getPreferences(TOKEN_PREFS);
        return prefs.getString(TOKEN_KEY, "");
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        if (logoTexture != null) logoTexture.dispose();
        // skin dispose samo, če ga ne uporabljaš drugje
    }
}
