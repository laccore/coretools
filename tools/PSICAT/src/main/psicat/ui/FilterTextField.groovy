/**
 * Created 10/15/2016 by CSD Facility.
 * 
 * Filtered text field with search icon and reset icon + functionality. Adaptation of
 * Georgios Migdos's nice JIconTextField example, found here:
 * 
 * https://gmigdos.wordpress.com/2010/03/30/java-a-custom-jtextfield-for-searching/
 *
 * Search and reset icons are user-defined.
 */

 // TODO: code is Java, but file is .groovy to avoid Java compilation issue in psicat/src.

package psicat.ui;
 
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
 
public class FilterTextField extends JTextField {
	private Icon searchIcon, resetIcon;
	private Insets defaultInsets;
 
	public FilterTextField(Icon searchIcon, Icon resetIcon) {
		super();
		this.searchIcon = searchIcon;
		this.resetIcon = resetIcon;
 
		Border border = UIManager.getBorder("TextField.border");
		JTextField dummy = new JTextField();
		this.defaultInsets = border.getBorderInsets(dummy);
		this.border = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.darkGray, 1), BorderFactory.createEmptyBorder(5, 20, 5, 20));
	}
 
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		// paint search icon 
		int iconWidth = searchIcon.getIconWidth();
		int iconHeight = searchIcon.getIconHeight();
		int x = 4; // fudgy - push just past left edge
		int y = (this.getHeight() - iconHeight)/2;
		searchIcon.paintIcon(this, g, x, y);
		 
		// paint reset icon
		if (!this.text.equals("")) {
			int resetY = (this.getHeight() - resetIcon.getIconHeight()) / 2;
			resetIcon.paintIcon(this, g, this.getWidth() - (resetIcon.getIconWidth() + defaultInsets.right), resetY);
		}
	}
	
	public void mouseClicked(MouseEvent e) {
		if (e.getPoint().x > this.getWidth() - 20)
			this.setText("");
	}
}