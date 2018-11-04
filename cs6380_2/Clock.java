
public class Clock {
	private int counter;
	public Clock() {
		counter = 0;
	}
	public void tick() {
		counter++;
	}
	public int read() {
		return counter;
	}
}
