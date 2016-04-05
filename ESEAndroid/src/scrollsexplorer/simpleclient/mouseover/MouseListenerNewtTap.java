package scrollsexplorer.simpleclient.mouseover;

import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.opengl.GLWindow;

public class MouseListenerNewtTap implements MouseListener
{
	// The canvas this handler is operating on
	private GLWindow glWindow;

	public MouseListenerNewtTap()
	{

	}

	public void setWindow(GLWindow newGlWindow)
	{
		// remove the old canvas listening
		if (glWindow != null)
		{
			glWindow.removeMouseListener(this);
		}

		glWindow = newGlWindow;
		if (glWindow != null)
		{
			glWindow.addMouseListener(this);
		}
	}

	public boolean hasGLWindow()
	{
		return glWindow != null;
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{

	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		for (int i = 0; i < e.getPointerCount(); i++)
		{
			int ex = e.getX(i);
			int ey = e.getY(i);
			int clickCount = e.getClickCount();
			System.out.println("mouse tappeded!  ");
			System.out.println("e.getClickCount() " + e.getClickCount());

		}
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
		mousePressed(e);
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
		mouseReleased(e);
	}

	@Override
	public void mouseMoved(MouseEvent e)
	{
		mousePressed(e);
	}

	@Override
	public void mouseDragged(MouseEvent e)
	{
		mousePressed(e);
	}

	@Override
	public void mouseWheelMoved(MouseEvent e)
	{
		System.out.println("What mouse wheel moved?");
	}

}
