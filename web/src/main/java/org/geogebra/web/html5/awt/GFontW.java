package org.geogebra.web.html5.awt;


public class GFontW extends org.geogebra.common.awt.GFont {
	public static final int BOLD = 1;
	public static final int ITALIC = 2;
	public static final String NORMAL_STR = "normal";
	public static final String BOLD_STR = "bold";
	public static final String ITALIC_STR = "italic";
	private String fontStyle = NORMAL_STR;
	private String fontVariant = NORMAL_STR;
	private String fontWeight = NORMAL_STR;
	private String fontSize = "12";
	private String lineHeight = "12";
	private String fontFamily = "geogebra-sans-serif, sans-serif";

	public GFontW(GFontW otherfont) {
		fontStyle = otherfont.getFontStyle();
		fontVariant = otherfont.getFontVariant();
		fontWeight = otherfont.getFontWeight();
		fontSize = otherfont.getFontSize();
		lineHeight = otherfont.getLineHeight();
		fontFamily = otherfont.getFontFamily();
	}

	public GFontW(String fontStyle) {
		this.setFontStyle(fontStyle);
	}

	public GFontW(String name, int style, int size) {
		if ("Serif".equals(name)) {
			fontFamily = "geogebra-serif, serif";
		} else if ("SansSerif".equals(name)) {
			fontFamily = "geogebra-sans-serif, sans-serif";
		} else {
			fontFamily = name;
		}
		fontSize = size + "";
		setFontStyle(style);

	}

	public void setFontStyle(String fontStyle) {
		this.fontStyle = fontStyle;
	}

	public String getFontStyle() {
		return fontStyle;
	}

	public String getFullFontString() {
		return fontStyle + " " + fontVariant + " " + fontWeight + " "
		        + fontSize + "px/" + lineHeight + "px " + fontFamily;
	}

	public void setFontVariant(String fontVariant) {
		this.fontVariant = fontVariant;
	}

	public String getFontVariant() {
		return fontVariant;
	}

	public void setFontWeight(String fontWeight) {
		this.fontWeight = fontWeight;
	}

	public String getFontWeight() {
		return fontWeight;
	}

	public void setFontSize(String fontSize) {
		this.fontSize = fontSize;
	}

	public String getFontSize() {
		return fontSize;
	}

	public void setLineHeight(String lineHeight) {
		this.lineHeight = lineHeight;
	}

	public String getLineHeight() {
		return lineHeight;
	}

	public void setFontFamily(String fontFamily) {
		this.fontFamily = fontFamily;
	}

	public String getFontFamily() {
		return fontFamily;
	}

	public GFontW deriveFont(String fontStyle2, final int newSize) {
		GFontW f = new GFontW(fontStyle2);
		f.setFontSize(newSize);
		return f;
	}

	@Override
	public int canDisplayUpTo(String textString) {
		// Suppose that everything will work well as it is difficult to
		// determine
		// if a character is displayable or not
		return -1;
	}

	public void setFontStyle(int fontStyle) {
		switch (fontStyle) {
		case BOLD:
			setFontWeight(BOLD_STR);
			setFontStyle(NORMAL_STR);
			break;
		case ITALIC:
			setFontWeight(NORMAL_STR);
			setFontStyle(ITALIC_STR);
			break;
		case (BOLD + ITALIC):
			setFontWeight(BOLD_STR);
			setFontStyle(ITALIC_STR);
			break;
		default:
			setFontStyle(NORMAL_STR);
			setFontWeight(NORMAL_STR);
		}
	}

	/**
	 * @param fontSize
	 *            font size
	 */
	public void setFontSize(int fontSize) {
		this.fontSize = "" + fontSize;
	}

	@Override
	public int getSize() {
		return Integer.parseInt(fontSize);
	}

	@Override
	public boolean isItalic() {
		return fontStyle.equals(ITALIC_STR);
	}

	@Override
	public boolean isBold() {
		return fontWeight.equals(BOLD_STR);
	}

	@Override
	public int getStyle() {
		return (isBold() ? BOLD : 0) + (isItalic() ? ITALIC : 0);
	}

	@Override
	public org.geogebra.common.awt.GFont deriveFont(int plain2, int fontSize) {
		GFontW ret = new GFontW(fontStyle);
		ret.setFontStyle(plain2);
		ret.setFontSize(fontSize);
		return ret;
	}

	@Override
	public org.geogebra.common.awt.GFont deriveFont(int plain2, float fontSize) {
		return deriveFont(plain2, (int) fontSize);
	}

	@Override
	public org.geogebra.common.awt.GFont deriveFont(int i) {
		GFontW ret = new GFontW(fontStyle);
		ret.setFontStyle(i);
		ret.setFontSize(fontSize);
		return ret;
	}

	@Override
	public String getFontName() {
		return fontFamily;
	}

	@Override
	public boolean equals(Object font) {
		if (font instanceof GFontW) {
			GFontW fontW = (GFontW) font;
			return fontFamily.equals(fontW.fontFamily)
			        && fontSize.equals(fontW.fontSize)
			        && fontStyle.equals(fontW.fontStyle)
			        && fontVariant.equals(fontW.fontVariant)
			        && fontWeight.equals(fontW.fontWeight)
			        && lineHeight.equals(fontW.lineHeight);

		}

		return false;
	}

}
