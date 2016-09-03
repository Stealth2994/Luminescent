package astechzgo.luminescent.entity;

import static astechzgo.luminescent.keypress.Key.KEYS_MOVEMENT_DOWN;
import static astechzgo.luminescent.keypress.Key.KEYS_MOVEMENT_FASTER;
import static astechzgo.luminescent.keypress.Key.KEYS_MOVEMENT_LEFT;
import static astechzgo.luminescent.keypress.Key.KEYS_MOVEMENT_RIGHT;
import static astechzgo.luminescent.keypress.Key.KEYS_MOVEMENT_UP;

import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

import astechzgo.luminescent.coordinates.GameCoordinates;
import astechzgo.luminescent.coordinates.ScaledWindowInvertedYAxisCoordinates;
import astechzgo.luminescent.coordinates.WindowCoordinates;
import astechzgo.luminescent.gameobject.Room;
import astechzgo.luminescent.rendering.Camera;
import astechzgo.luminescent.rendering.CircularObjectRenderer;
import astechzgo.luminescent.rendering.IObjectRenderer;
import astechzgo.luminescent.textures.Texture;
import astechzgo.luminescent.utils.Constants;
import astechzgo.luminescent.utils.ControllerUtils;
import astechzgo.luminescent.utils.DisplayUtils;
import astechzgo.luminescent.utils.KeyboardUtils;

public class Player extends LivingEntity {

	private double radius;
	private double rotation;
	
	private final PlayerRenderer renderer;
	
	public static final double slowSpeed = 0.5;
	public static final double fastSpeed = 0.88;

	private double lastDelta;
	
	private double lastControllerDelta = 0;
	private ScaledWindowInvertedYAxisCoordinates lastMouseCoords;
	
	public Player() {
		renderer = new PlayerRenderer(new WindowCoordinates(new GameCoordinates(Camera.CAMERA_WIDTH / 2, Camera.CAMERA_HEIGHT / 2)), 40, 1);
		lastDelta = GLFW.glfwGetTime() * 1000;
		lastControllerDelta = GLFW.glfwGetTime() * 1000;
		
		radius = 40;
		coordinates = new GameCoordinates(Camera.CAMERA_WIDTH / 2, Camera.CAMERA_HEIGHT / 2);
	}
	
	@Override
	public GameCoordinates getCoordinates() {
		return coordinates;
	}
	
	public void setCoordinates(GameCoordinates coordinates) {
		super.coordinates = coordinates;
	}
	
	public void setRadius(double radius) {
		this.radius = radius;
	}
	
	public double getRadius() {
		return radius;
	}
	
	public double setRotation() {
		double delta = ((GLFW.glfwGetTime() * 1000) - lastControllerDelta);
		lastControllerDelta = GLFW.glfwGetTime() * 1000;
		
		if(ControllerUtils.isButtonPressed(Constants.CONTROLLER_MOVEMENT_ROTATION_CLOCKWISE)) {
			rotation = rotation + 0.5 * delta;
			
			return rotation;
		}
		if(ControllerUtils.isButtonPressed(Constants.CONTROLLER_MOVEMENT_ROTATION_COUNTERCLOCKWISE)) {
			rotation = rotation - 0.5 * delta;
			return rotation;
		}

		ScaledWindowInvertedYAxisCoordinates mouseCoords = null;
		
		try(MemoryStack stack = MemoryStack.stackPush()) {	
			DoubleBuffer mxpos = stack.mallocDouble(1);
			DoubleBuffer mypos = stack.mallocDouble(1);
		
			GLFW.glfwGetCursorPos(DisplayUtils.getHandle(), mxpos, mypos);
		
			mouseCoords = new ScaledWindowInvertedYAxisCoordinates(mxpos.get(0) - DisplayUtils.widthOffset, mypos.get(0) - DisplayUtils.heightOffset);
		
			if(mouseCoords.equals(lastMouseCoords)) 
				return rotation;
		
			lastMouseCoords = mouseCoords;
		
			mxpos.clear();
			mypos.clear();
		}
		double m = (coordinates.getAbsoluteY() - mouseCoords.getAbsoluteY()) / (coordinates.getAbsoluteX() - mouseCoords.getAbsoluteX());
		
		if(m == Double.POSITIVE_INFINITY) {
			rotation = 0;
			return rotation;
		}
		
		if(m == Double.NEGATIVE_INFINITY) {
			rotation = 180;
			return rotation;
		}
		if(mouseCoords.getAbsoluteX() == coordinates.getAbsoluteX() && mouseCoords.getAbsoluteY() == coordinates.getAbsoluteY()) {
			return rotation;
		}

		if(mouseCoords.getAbsoluteX() < coordinates.getAbsoluteX())
			rotation = 90 - Math.toDegrees(Math.atan(m));
		else {
			if(270 - Math.toDegrees(Math.atan(m)) > 360)
				rotation = 270 - Math.toDegrees(Math.atan(m)) - 360;
			else
				rotation = 270 - Math.toDegrees(Math.atan(m));
		}
		
		return rotation;
	}
	public void move(List<Room> rooms) {

		double delta = ((GLFW.glfwGetTime() * 1000) - lastDelta);
		lastDelta = GLFW.glfwGetTime() * 1000;
		
		double speed = 0;
		
		if(KEYS_MOVEMENT_FASTER.isKeyDown()) {
			speed = Player.fastSpeed * delta;
		}
		else {
			speed = Player.slowSpeed * delta;
		}	
		
		boolean down = true;
		
		double tempAngle = -1;
		
		if(!(KEYS_MOVEMENT_UP.isKeyDown()) == KEYS_MOVEMENT_DOWN.isKeyDown()) {
			down = false;
			
			if(KeyboardUtils.isKeyDown(Constants.KEYS_MOVEMENT_UP))
				tempAngle = 90;
			else
				tempAngle = 270;
		}
		if(!(KEYS_MOVEMENT_LEFT.isKeyDown() == KEYS_MOVEMENT_RIGHT.isKeyDown())) {
			down = false;
			
			if(KEYS_MOVEMENT_LEFT.isKeyDown())
				if(tempAngle == -1)
					tempAngle =  180;
				else if(KeyboardUtils.isKeyDown(Constants.KEYS_MOVEMENT_UP))
					tempAngle = (tempAngle + 360 + 270);
				else
					tempAngle = (tempAngle + 180) / 2;
			else
				if(tempAngle == -1)
					tempAngle =  0;
				else if(KeyboardUtils.isKeyDown(Constants.KEYS_MOVEMENT_UP))
					tempAngle = tempAngle / 2;
				else
					tempAngle = (tempAngle + 360) / 2;
		}
					
		if(down)
			speed = 0;		
		
		if(tempAngle == -1) tempAngle = 90;
		
		double angle = setRotation() + tempAngle;
		double x = 0;
		double z = 0;
		
		boolean bx = false;
		boolean bz = false;
		
		boolean initX = true;
		boolean initZ = true;
		for(Room room : rooms) {
			double projectedX = this.getCoordinates().getGameCoordinatesX() + speed * Math.cos(Math.toRadians(angle));
			if(projectedX > getCoordinates().getGameCoordinatesX()) {
				projectedX += this.radius;
			}
			else {
				projectedX -= this.radius;
			}
		
			double projectedY = getCoordinates().getGameCoordinatesZ() - speed * Math.sin(Math.toRadians(angle));	
			if(projectedY > getCoordinates().getGameCoordinatesZ()) {
				projectedY += this.radius;
			}
			else {
				projectedY -= this.radius;
			}
			double roomx = new GameCoordinates(room.getCoordinates()).getGameCoordinatesX();
			double roomy = new GameCoordinates(room.getCoordinates()).getGameCoordinatesZ();
			if((projectedX < roomx  || projectedX > roomx + room.getWidth() ) || (projectedY < roomy|| projectedY > roomy + room.getHeight())) {
			
				if(projectedY > roomy && projectedY < roomy + room.getHeight() && getCoordinates().getGameCoordinatesX() > roomx - 0.1 && getCoordinates().getGameCoordinatesX() < roomx + room.getWidth() + 0.1) {
				bx = true;
				}
	if(projectedX > roomx && projectedX < roomx + room.getWidth() && getCoordinates().getGameCoordinatesZ() > roomy -0.1 && getCoordinates().getGameCoordinatesZ() < roomy + room.getHeight() + 0.1) {	
		bz = true;
				}
			}
			else {
				double tempz = getCoordinates().getGameCoordinatesZ();
				this.setCoordinates(new GameCoordinates(getCoordinates().getGameCoordinatesX() + speed * Math.cos(Math.toRadians(angle)), tempz));
				double tempx = getCoordinates().getGameCoordinatesX();
				setCoordinates(new GameCoordinates(tempx, getCoordinates().getGameCoordinatesZ() - speed * Math.sin(Math.toRadians(angle))));
				bx = false;
				bz = false;
			break;
			}
		}
	if(bx) {
		double tempx = getCoordinates().getGameCoordinatesX();
		setCoordinates(new GameCoordinates(tempx, getCoordinates().getGameCoordinatesZ() - speed * Math.sin(Math.toRadians(angle))));
	}
	if(bz) {
		double tempz = getCoordinates().getGameCoordinatesZ();
		this.setCoordinates(new GameCoordinates(getCoordinates().getGameCoordinatesX() + speed * Math.cos(Math.toRadians(angle)), tempz));
	}
	}
	
	// TODO: Simplify edges
	private List<Double> getVerticalEdges(List<Room> rooms) {
		List<Double> verticalEdges = new ArrayList<Double>();
	
		for(Room room : rooms) {
			verticalEdges.add(new GameCoordinates(room.getCoordinates()).getGameCoordinatesX());
			verticalEdges.add(new GameCoordinates(room.getCoordinates()).getGameCoordinatesX() + room.getWidth());
		}
		
		return verticalEdges;
	}
	
	private List<Double> getHorizontalEdges(List<Room> rooms) {
		List<Double> horizontalEdges = new ArrayList<Double>();
		
		for(Room room : rooms) {
			horizontalEdges.add(new GameCoordinates(room.getCoordinates()).getGameCoordinatesZ() + room.getHeight());
			horizontalEdges.add(new GameCoordinates(room.getCoordinates()).getGameCoordinatesZ());
		}
		
		return horizontalEdges;
	}

	@Override
	public IObjectRenderer getRenderer() {
		return renderer;
	}
	
	private class PlayerRenderer extends CircularObjectRenderer {
		
		private PlayerRenderer(WindowCoordinates coordinates, double radius) {
			super(coordinates, radius);
		}
		
		private PlayerRenderer(WindowCoordinates coordinates, double radius, int pointSeperation) {
			super(coordinates, radius, pointSeperation);
		}
		
		private PlayerRenderer(WindowCoordinates coordinates, double radius, int pointSeperation, Texture texture) {
			super(coordinates, radius, pointSeperation, texture);
		}
		
		private PlayerRenderer(WindowCoordinates coordinates, double radius, Texture texture) {
			super(coordinates, radius, texture);
		}
		
		@Override
		public void resize() {
			coordinates = new WindowCoordinates(Camera.CAMERA_WIDTH / 2, Camera.CAMERA_HEIGHT / 2);
					
			scaledRadius = (int) Math
					.round((double) radius / Camera.CAMERA_WIDTH * (DisplayUtils.getDisplayWidth() - DisplayUtils.widthOffset * 2));

			oldGameWidth = DisplayUtils.getDisplayWidth() - DisplayUtils.widthOffset * 2;
			oldGameHeight = DisplayUtils.getDisplayHeight() - DisplayUtils.heightOffset * 2;
		}
		
		private void setRotation(double rotation) {
			this.rotation = rotation;
		}
	}

	@Override
	public void updateRenderer() {
		renderer.setRotation(rotation);
		renderer.setCoordinates(new WindowCoordinates(coordinates));
	}
}