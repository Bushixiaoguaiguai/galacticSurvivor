package com.lucas.galacticsurvivor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Locale;

class GameScreen implements Screen{

    //screen
    private Camera camera;
    private Viewport viewport;

    //graphics
    private SpriteBatch batch;
    private TextureAtlas textureAtlas;
    private Texture explosionTexture;

    private TextureRegion[] backgrounds;
    private float backgroundHeight;

    private TextureRegion playerShipTextureRegion, playerShieldTextureRegion,
            enemyShipTextureRegion, enemyShieldTextureRegion,
    playerLaserTextureRegion, enemyLaserTextureRegion;

    //timing
    private float[] backgroundOffsets = {0,0,0,0};
    private float backgroundMaxScrollingSpeed;
    private float timeBetweenEnemySpawns = 1f;
    private float enemySpawnTimer =0;

    //world parameters
    private final float WORLD_WIDTH = 72;
    private final float WORLD_HEIGHT = 128;
    private final float TOUCH_MOVEMENT_THRESHOLD = 0.5f;

    //game object
    private PlayerShip playerShip;
    private LinkedList<EnemyShip> enemyShipList;
    private LinkedList<Laser> playerLaserList;
    private LinkedList<Laser> enemyLaserList;
    private LinkedList<Explosion> explosionList;

    private int score = 0;

    //Heads-up Display
    BitmapFont font;
    float hudVerticalMargin, hudLeftX, hudRightX, hudCentereX, hudRow1Y, hudRow2Y, hudSectionWidth;

    GameScreen(){

        camera = new OrthographicCamera();
        viewport = new StretchViewport(WORLD_WIDTH,WORLD_HEIGHT,camera);

        //setting up the texture atlas
        textureAtlas = new TextureAtlas("images.atlas");
        //setting up the background
        backgrounds = new TextureRegion[4];
        backgrounds[0] = textureAtlas.findRegion("1");
        backgrounds[1] = textureAtlas.findRegion("2");
        backgrounds[2] = textureAtlas.findRegion("3");
        backgrounds[3] = textureAtlas.findRegion("4");

        backgroundHeight = WORLD_HEIGHT *2;
        backgroundMaxScrollingSpeed = (float)WORLD_HEIGHT /4;

        //initialize texture regions
        playerShipTextureRegion = textureAtlas.findRegion("playerShip2_blue");
        enemyShipTextureRegion = textureAtlas.findRegion("enemyRed3");
        playerShieldTextureRegion = textureAtlas.findRegion("shield2");
        enemyShieldTextureRegion = textureAtlas.findRegion("shield1");
        playerLaserTextureRegion = textureAtlas.findRegion("laserBlue02");
        enemyLaserTextureRegion = textureAtlas.findRegion("laserRed02");
        enemyShipTextureRegion.flip(false,true);
        enemyShieldTextureRegion.flip(false,true);

        explosionTexture = new Texture("explosion.png");

        //set up game objects
        playerShip = new PlayerShip(WORLD_WIDTH/2, WORLD_HEIGHT/4,
                10,10,
                36,3,
                0.9f,4,
                45, 0.5f,
                playerShipTextureRegion,playerShieldTextureRegion,playerLaserTextureRegion);

        enemyShipList = new LinkedList<>();

        playerLaserList = new LinkedList<>();
        enemyLaserList = new LinkedList<>();

        explosionList = new LinkedList<>();

        batch = new SpriteBatch();

        prepareHUD();
    }

    private void prepareHUD(){
        //create a BitmapFont from our font file
        FreeTypeFontGenerator fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("EdgeOfTheGalaxyItalic-ZVJB3.otf"));
        FreeTypeFontGenerator.FreeTypeFontParameter fontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();

        fontParameter.size = 72;
        fontParameter.borderWidth = 3.6f;
        fontParameter.color = new Color(1,1,1,0.3f);
        fontParameter.borderColor = new Color(0,0,0,0.3f);

        font = fontGenerator.generateFont(fontParameter);

        //scale the font to fit world
        font.getData().setScale(0.08f);

        //calculate hud margins, etc.
        hudVerticalMargin = font.getCapHeight()/2;
        hudLeftX = hudVerticalMargin;
        hudRightX = WORLD_WIDTH *2/3 - hudLeftX;
        hudCentereX = WORLD_WIDTH /3;
        hudRow1Y = WORLD_HEIGHT - hudVerticalMargin;
        hudRow2Y = hudRow1Y - hudVerticalMargin - font.getCapHeight();
        hudSectionWidth = WORLD_WIDTH/3;
    }

    @Override
    public void render(float deltaTime) {
        batch.begin();

        //main menu


        //scrolling background
        renderBackground(deltaTime);

        detectInput(deltaTime);
        playerShip.update(deltaTime);

        spawnEnemyShips(deltaTime);

        ListIterator<EnemyShip> enemyShipListIterator = enemyShipList.listIterator();
        while (enemyShipListIterator.hasNext()) {
            EnemyShip enemyShip = enemyShipListIterator.next();
            moveEnemies(enemyShip, deltaTime);
            enemyShip.update(deltaTime);

            //enemy ships
            enemyShip.draw(batch);
        }

        //player ships
        playerShip.draw(batch);

        //lasers
        renderLasers(deltaTime);

        //detect collisions between lasers and ships
        detectCollisions();

        //explosions
        renderExplosions(deltaTime);

        //hud rendering
        updateAndRenderHUD();

        batch.end();
    }

    private void updateAndRenderHUD(){
        //render top row labels
        font.draw(batch,"Score", hudLeftX, hudRow1Y, hudSectionWidth, Align.left, false);
        font.draw(batch,"Shield", hudCentereX, hudRow1Y, hudSectionWidth, Align.center, false);
        font.draw(batch,"Lives", hudRightX, hudRow1Y, hudSectionWidth, Align.right, false);

        //render second row values
        font.draw(batch, String.format(Locale.getDefault(), "%06d", score), hudLeftX, hudRow2Y, hudSectionWidth, Align.left, false);
        font.draw(batch, String.format(Locale.getDefault(), "%02d", playerShip.shield), hudCentereX, hudRow2Y, hudSectionWidth, Align.center, false);
        font.draw(batch, String.format(Locale.getDefault(), "%02d", playerShip.lives), hudRightX, hudRow2Y, hudSectionWidth, Align.right, false);
    }

    private void spawnEnemyShips(float deltaTime){
        enemySpawnTimer += deltaTime;

        if(enemySpawnTimer > timeBetweenEnemySpawns){
            enemyShipList.add(new EnemyShip(
                    GalacticSurvivorGame.random.nextFloat()*(WORLD_WIDTH-10), WORLD_HEIGHT,
                    10,10,
                    40,0,
                    0.5f,5,
                    50, 0.8f,
                    enemyShipTextureRegion,enemyShieldTextureRegion,enemyLaserTextureRegion));
            enemySpawnTimer -= timeBetweenEnemySpawns;
        }

    }

    private void detectInput(float deltaTime){
        //keyboard input

        //strategy: determine the max distance the ship can move
        //check each key that matters and move accordingly

        float leftLimit, rightLimit, upLimit, downLimit;
        leftLimit = -playerShip.boundingBox.x;
        downLimit = -playerShip.boundingBox.y;
        rightLimit = WORLD_WIDTH - playerShip.boundingBox.x - playerShip.boundingBox.width;
        upLimit = (float)WORLD_HEIGHT - playerShip.boundingBox.y - playerShip.boundingBox.height;


        //touch input (also mouse)
        if(Gdx.input.isTouched()){
            //get the screen position of the touch
            float xTouchPixels = Gdx.input.getX();
            float yTouchPixels = Gdx.input.getY();

            //convert to world position
            Vector2 touchPoint = new Vector2(xTouchPixels,yTouchPixels);
            touchPoint = viewport.unproject(touchPoint);

            //calculate the x and y differences
            Vector2 playerShipCentre = new Vector2(
                    playerShip.boundingBox.x+(playerShip.boundingBox.width/2),
                    playerShip.boundingBox.y+(playerShip.boundingBox.height/2));

            float touchDistance = touchPoint.dst(playerShipCentre);

            if(touchDistance > TOUCH_MOVEMENT_THRESHOLD){
                float xTouchDifference = touchPoint.x - playerShipCentre.x;
                float yTouchDifference = touchPoint.y - playerShipCentre.y;

                //scale to the max speed of the ship
                float xMove = xTouchDifference / touchDistance * playerShip.movementSpeed *deltaTime;
                float yMove = yTouchDifference / touchDistance * playerShip.movementSpeed *deltaTime;

                if(xMove > 0) xMove = Math.min(xMove,rightLimit);
                else xMove = Math.max(xMove,leftLimit);

                if(yMove > 0) yMove = Math.min(yMove,upLimit);
                else yMove = Math.max(yMove,downLimit);

                playerShip.translate(xMove,yMove);
            }
        }
        else{
            if(Gdx.input.isKeyPressed(Input.Keys.D) && rightLimit > 0){
                playerShip.translate(Math.min(playerShip.movementSpeed * deltaTime, rightLimit), 0f);
            }
            if(Gdx.input.isKeyPressed(Input.Keys.A) && leftLimit < 0){
                playerShip.translate(Math.max(-playerShip.movementSpeed * deltaTime, leftLimit), 0f);
            }
            if(Gdx.input.isKeyPressed(Input.Keys.W) && upLimit > 0){
                playerShip.translate(0f, Math.min(playerShip.movementSpeed * deltaTime, upLimit));
            }
            if(Gdx.input.isKeyPressed(Input.Keys.S) && downLimit < 0){
                playerShip.translate(0f, Math.max(-playerShip.movementSpeed * deltaTime, downLimit));
            }
        }

    }

    private void moveEnemies(EnemyShip enemyShip, float deltaTime){
        float leftLimit, rightLimit, upLimit, downLimit;
        leftLimit = -enemyShip.boundingBox.x;
        downLimit = (float)WORLD_HEIGHT/2-enemyShip.boundingBox.y;
        rightLimit = WORLD_WIDTH - enemyShip.boundingBox.x - enemyShip.boundingBox.width;
        upLimit = WORLD_HEIGHT - enemyShip.boundingBox.y - enemyShip.boundingBox.height;

        float xMove = enemyShip.getDirectionVector().x * enemyShip.movementSpeed *deltaTime;
        float yMove = enemyShip.getDirectionVector().y * enemyShip.movementSpeed *deltaTime;

        if(xMove > 0) xMove = Math.min(xMove,rightLimit);
        else xMove = Math.max(xMove,leftLimit);

        if(yMove > 0) yMove = Math.min(yMove,upLimit);
        else yMove = Math.max(yMove,downLimit);

        enemyShip.translate(xMove,yMove);
    }

    private void detectCollisions(){
        //for each player laser, check whether it intersects an enemy ship
        ListIterator<Laser> laserListiterator = playerLaserList.listIterator();
        while(laserListiterator.hasNext()) {
            Laser laser = laserListiterator.next();
            ListIterator<EnemyShip> enemyShipListIterator = enemyShipList.listIterator();
            while (enemyShipListIterator.hasNext()) {
                EnemyShip enemyShip = enemyShipListIterator.next();

                if (enemyShip.intersects(laser.boundingBox)) {
                    //contact with enemy ship
                    if(enemyShip.hitAndCheckDestroyed(laser)){
                        enemyShipListIterator.remove();
                        explosionList.add(new Explosion(explosionTexture,
                                new Rectangle(enemyShip.boundingBox),
                                0.7f));
                        score += 10;
                    }
                    laserListiterator.remove();
                    break;
                }
            }
        }

        //for each enemy laser, check whether it intersects an player ship
        laserListiterator = enemyLaserList.listIterator();
        while(laserListiterator.hasNext()){
            Laser laser = laserListiterator.next();
            if(playerShip.intersects(laser.boundingBox)){
                //contact with player ship
                if(playerShip.hitAndCheckDestroyed(laser)){
                    explosionList.add(new Explosion(explosionTexture,
                            new Rectangle(playerShip.boundingBox),
                            1.6f));
                    playerShip.shield = 3;
                    playerShip.lives--;
                }
                laserListiterator.remove();
                break;
            }
        }
    }

    private void renderExplosions(float deltaTime){
        ListIterator<Explosion> explosionListIterator = explosionList.listIterator();
        while(explosionListIterator.hasNext()){
            Explosion explosion = explosionListIterator.next();
            explosion.update(deltaTime);
            if(explosion.isFinished()){
                explosionListIterator.remove();
            }
            else{
                explosion.draw(batch);
            }
        }
    }

    private void renderLasers(float deltaTime){
        //create new lasers
        //player lasers
        if(playerShip.canFireLaser()){
            Laser[] lasers = playerShip.fireLasers();
            for(Laser laser: lasers){
                playerLaserList.add(laser);
            }
        }
        //enemy lasers
        ListIterator<EnemyShip> enemyShipListIterator = enemyShipList.listIterator();
        while (enemyShipListIterator.hasNext()) {
            EnemyShip enemyShip = enemyShipListIterator.next();
            if (enemyShip.canFireLaser()) {
                Laser[] lasers = enemyShip.fireLasers();
                for (Laser laser : lasers) {
                    enemyLaserList.add(laser);
                }
            }
        }

        //draw lasers
        //remove old lasers
        ListIterator<Laser> iterator = playerLaserList.listIterator();
        while(iterator.hasNext()){
            Laser laser = iterator.next();
            laser.draw(batch);
            laser.boundingBox.y += laser.movementSpeed*deltaTime;
            if(laser.boundingBox.y > WORLD_HEIGHT) {
                iterator.remove();
            }
        }

        iterator = enemyLaserList.listIterator();
        while(iterator.hasNext()){
            Laser laser = iterator.next();
            laser.draw(batch);
            laser.boundingBox.y -= laser.movementSpeed*deltaTime;
            if(laser.boundingBox.y + laser.boundingBox.height < 0) {
                iterator.remove();
            }
        }
    }

    private void renderBackground(float deltaTime){
        backgroundOffsets[0] += deltaTime*backgroundMaxScrollingSpeed /8;
        backgroundOffsets[1] += deltaTime*backgroundMaxScrollingSpeed /4;
        backgroundOffsets[2] += deltaTime*backgroundMaxScrollingSpeed /2;
        backgroundOffsets[3] += deltaTime*backgroundMaxScrollingSpeed;

        for(int layer = 0; layer <backgroundOffsets.length; layer++){
            if (backgroundOffsets[layer] > WORLD_HEIGHT){
                backgroundOffsets[layer] = 0;
            }
            batch.draw(backgrounds[layer],0,-backgroundOffsets[layer],WORLD_WIDTH,backgroundHeight);
//            batch.draw(backgrounds[layer],0,-backgroundOffsets[layer]+WORLD_HEIGHT,WORLD_WIDTH,WORLD_HEIGHT);
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height,true);
        batch.setProjectionMatrix(camera.combined);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void show() {

    }

    @Override
    public void dispose() {
        batch.dispose();
    }
}
