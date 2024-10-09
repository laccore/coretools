/*
 * Copyright (c) Josh Reed, 2009.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.andrill.coretools.graphics.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Paper sizes.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class Paper {
	private static Map<String, Paper> PAGES = new HashMap<String, Paper>();
	private static ArrayList<Paper> PAGES_LIST = new ArrayList<Paper>();
	private static final int DEFAULT_MARGIN = 36; // 0.5"

	// Default to US Letter (8.5" x 11") at double resolution (144dpi), which works
	// well with the new columns added in PSICAT v1.2.0.
	public static final Paper DEFAULT = new Paper("Letter (144dpi)", 1224, 1584, DEFAULT_MARGIN);
	
	public static final Paper A0 = new Paper("A0", 2384, 3371, DEFAULT_MARGIN);
	public static final Paper A1 = new Paper("A1", 1685, 2384, DEFAULT_MARGIN);
	public static final Paper A2 = new Paper("A2", 1190, 1684, DEFAULT_MARGIN);
	public static final Paper A3 = new Paper("A3", 842, 1190, DEFAULT_MARGIN);
	public static final Paper A4 = new Paper("A4", 595, 842, DEFAULT_MARGIN);
	public static final Paper A5 = new Paper("A5", 420, 595, DEFAULT_MARGIN);
	public static final Paper B4 = new Paper("B4", 729, 1032, DEFAULT_MARGIN);
	public static final Paper B5 = new Paper("B5", 516, 729, DEFAULT_MARGIN);

	public static final Paper EXECUTIVE = new Paper("Executive", 540, 720, DEFAULT_MARGIN);
	public static final Paper FOLIO = new Paper("Folio", 612, 936, DEFAULT_MARGIN);
	public static final Paper LEDGER = new Paper("Ledger", 1224, 792, DEFAULT_MARGIN);
	public static final Paper LEGAL = new Paper("Legal", 612, 1008, DEFAULT_MARGIN);
	public static final Paper LETTER = new Paper("Letter (72dpi)", 612, 792, DEFAULT_MARGIN);
	public static final Paper QUARTO = new Paper("Quarto", 610, 780, DEFAULT_MARGIN);
	public static final Paper STATEMENT = new Paper("Statement", 396, 612, DEFAULT_MARGIN);
	public static final Paper TABLOID = new Paper("Tabloid", 792, 1224, DEFAULT_MARGIN);

	static {
		PAGES_LIST.add(DEFAULT);
		PAGES_LIST.add(LETTER);
		PAGES_LIST.add(LEGAL);
		PAGES_LIST.add(A0);
		PAGES_LIST.add(A1);
		PAGES_LIST.add(A2);
		PAGES_LIST.add(A3);
		PAGES_LIST.add(A4);
		PAGES_LIST.add(A5);
		PAGES_LIST.add(B4);
		PAGES_LIST.add(B5);
		PAGES_LIST.add(TABLOID);
		PAGES_LIST.add(LEDGER);
		PAGES_LIST.add(STATEMENT);
		PAGES_LIST.add(EXECUTIVE);
		PAGES_LIST.add(FOLIO);
		PAGES_LIST.add(QUARTO);
	}
	
	static {
		PAGES.put("default", DEFAULT);
		PAGES.put("letter", LETTER);
		PAGES.put("legal", LEGAL);
		PAGES.put("a0", A0);
		PAGES.put("a1", A1);
		PAGES.put("a2", A2);
		PAGES.put("a3", A3);
		PAGES.put("a4", A4);
		PAGES.put("a5", A5);
		PAGES.put("b4", B4);
		PAGES.put("b5", B5);
		PAGES.put("tabloid", TABLOID);
		PAGES.put("ledger", LEDGER);
		PAGES.put("statement", STATEMENT);
		PAGES.put("executive", EXECUTIVE);
		PAGES.put("folio", FOLIO);
		PAGES.put("quarto", QUARTO);
	}

	/**
	 * Gets the page associated with the specified name.
	 * 
	 * @param name
	 *            the name.
	 * @return the page.
	 */
	public static Paper get(final String name) {
		// no name specified
		if ((name == null) || "".equals(name.trim())) {
			return getDefault();
		}

		// try looking up by name
		final Paper page = PAGES.get(name.trim().toLowerCase());
		if (page == null) {
			// try parsing the paper format
			String[] split = name.replaceAll("[\\[\\]x\\+]", " ").split(" ");
			if (split.length == 6) {
				int w = Integer.parseInt(split[0]);
				int h = Integer.parseInt(split[1]);
				int pw = Integer.parseInt(split[2]);
				int ph = Integer.parseInt(split[3]);
				int px = Integer.parseInt(split[4]);
				int py = Integer.parseInt(split[5]);
				return new Paper(name, w, h, pw, ph, px, py);
			} else {
				return getDefault();
			}
		} else {
			return page;
		}
	}

	public static Paper getDefault() {
		final String country = Locale.getDefault().getCountry();
		if ("US".equals(country) || "CA".equals(country)) {
			return LETTER;
		} else {
			return A4;
		}
	}

	private final String name;
	private final int px, py, pw, ph;
	private final int width, height;

	/**
	 * Create a new page with the specified width, height, and uniform margin all measured in points.
	 * 
	 * @param width
	 *            the width.
	 * @param height
	 *            the height.
	 * @param margin
	 *            the margin.
	 */
	public Paper(final String name, final int width, final int height, final int margin) {
		this.name = name;
		this.width = width;
		this.height = height;
		px = margin;
		py = margin;
		pw = width - 2 * margin;
		ph = height - 2 * margin;
	}

	/**
	 * Create a new page with the specified width, height, and margins all measured in points.
	 * 
	 * @param width
	 *            the width.
	 * @param height
	 *            the height.
	 * @param printableWidth
	 *            the printable width.
	 * @param printableHeight
	 *            the printable height.
	 * @param left
	 *            the left margin.
	 * @param top
	 *            the top margin.
	 */
	public Paper(final String name, final int width, final int height, final int printableWidth, final int printableHeight,
	        final int left, final int top) {
		this.name = name;
		this.width = width;
		this.height = height;
		px = left;
		py = top;
		pw = printableWidth;
		ph = printableHeight;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Paper other = (Paper) obj;
		if (!name.equals(other.name)) {
			return false;
		}
		if (height != other.height) {
			return false;
		}
		if (ph != other.ph) {
			return false;
		}
		if (pw != other.pw) {
			return false;
		}
		if (px != other.px) {
			return false;
		}
		if (py != other.py) {
			return false;
		}
		if (width != other.width) {
			return false;
		}
		return true;
	}

	public String getName() { return name; }

	/**
	 * Gets the page height in points.
	 * 
	 * @return the height.
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * Gets the printable height in points.
	 * 
	 * @return the printable height.
	 */
	public int getPrintableHeight() {
		return ph;
	}

	/**
	 * Gets the printable width in points.
	 * 
	 * @return the printable width.
	 */
	public int getPrintableWidth() {
		return pw;
	}

	/**
	 * Gets the printable x in points.
	 * 
	 * @return the printable x.
	 */
	public int getPrintableX() {
		return px;
	}

	/**
	 * Gets the printable y in points.
	 * 
	 * @return the printable y.
	 */
	public int getPrintableY() {
		return py;
	}

	/**
	 * Gets the page width in points.
	 * 
	 * @return the page width.
	 */
	public int getWidth() {
		return width;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + height;
		result = prime * result + ph;
		result = prime * result + pw;
		result = prime * result + px;
		result = prime * result + py;
		result = prime * result + width;
		return result;
	}

	@Override
	public String toString() {
		// find existing paper
		for (Entry<String, Paper> entry : PAGES.entrySet()) {
			if (entry.getValue().equals(this)) {
				Paper p = (Paper)entry.getValue();
				return p.name + " (" + p.width + " x " + p.height + " pixels)";
			}
		}
		// otherwise return a string representation
		return name + ": " + width + "x" + height + "[" + pw + "x" + ph + "+" + px + "+" + py + "]";
	}
}
