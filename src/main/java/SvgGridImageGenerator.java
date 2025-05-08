import org.apache.batik.transcoder.*;
import org.apache.batik.transcoder.image.PNGTranscoder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.List;

/** <b>SVG Grid Image Generator - by hoteymaks</b>
 * <p>Searches for all SVG's in the directory it is opened in and generates square-alike grid images with SVG previews.
 * <p>Output file name: {@code svg_grid_output.png}
 * <p>Supports {@code main()} initialization and static methods for:
 * <ul>
 *     <li>{@code generateSvgGridImage()} - default path (project directory), numerating cells by default (1, 2, 3...)</li>
 *     <li>{@code generateSvgGridImage(LabelMode)} - default path, picking custom labeling mode: NONE, FILE_NAME or NUMERATE</li>
 *     <li>{@code generateSvgGridImage(Path)} - custom path, numerating cells by default (1, 2, 3...)</li>
 *     <li>{@code generateSvgGridImage(Path, LabelMode)} - custom path, custom labeling mode: NONE, FILE_NAME or NUMERATE</li>
 * </ul>
 **/
public class SvgGridImageGenerator {

    // Change the labeling mode: NONE - no label, NUMERATE - 1, 2, 3..., FILE_NAME - writes file name in each cell
    private static final LabelMode MODE = LabelMode.NUMERATE;

    // Cell dimensions
    private static final int CELL_WIDTH = 250;
    private static final int CELL_HEIGHT = 250;

    // Customize design or leave as it is (by default)
    private static final int PADDING = 10;
    private static final int NUMBER_PADDING = 5;
    private static final int FONT_SIZE = 20;
    private static final String FONT_NAME = "Arial";
    private static final Color NUMBER_COLOR = Color.BLACK;
    private static final Color BORDER_COLOR = new Color(211, 211, 211);

    // Recommended not to be turned off. Change to "false" only if your custom font displays too smoothly
    private static final boolean ANTI_ALIASING = true;

    // Terminal colors, does not affect the output
    private static final String RESET = "\033[0m";
    private static final String GREEN = "\033[32m";
    private static final String RED = "\033[31m";
    private static final String YELLOW = "\033[33m";
    private static final String BLUE = "\033[34m";

    public static void main(String[] args) throws Exception {
        generateSvgGridImage(MODE);
    }

    /** Output path: {@code DEFAULT} - in project directory
     * <p>Label mode is {@code DEFAULT} - numerating **/
    public static void generateSvgGridImage() throws Exception {
        Path path = Paths.get(System.getProperty("user.dir"));
        generateSvgGridImage(path, LabelMode.NUMERATE);
    }

    /** Output path: {@code Path}
     * <p>Label mode is {@code DEFAULT} - numerating **/
    public static void generateSvgGridImage(Path path) throws Exception {
        generateSvgGridImage(path, LabelMode.NUMERATE);
    }

    /** Label mode: {@code LabelMode}
     * <p>Output path is {@code DEFAULT} - in project directory
     * <p>Use {@code LabelMode.NONE} for no labels, {@code LabelMode.NUMERATE} for numerated output,
     * {@code LabelMode.FILE_NAME} to display file names instead of numerating **/
    public static void generateSvgGridImage(LabelMode mode) throws Exception {
        Path path = Paths.get(System.getProperty("user.dir"));
        generateSvgGridImage(path, mode);
    }

    /** Output path: {@code Path}
     * <p>Label mode: {@code LabelMode}
     * <p>Use {@code LabelMode.NONE} for no labels, {@code LabelMode.NUMERATE} for numerated output,
     * {@code LabelMode.FILE_NAME} to display file names instead of numerating **/
    public static void generateSvgGridImage(Path path, LabelMode mode) throws Exception {
        System.out.println(GREEN + "Searching for .svg files..." + RESET);

        List<Path> svgFiles = Files.walk(path)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().toLowerCase().endsWith(".svg"))
                .toList();

        if (svgFiles.isEmpty()) {
            System.out.println(RED + "No SVG files found." + RESET);
            return;
        }

        int total = svgFiles.size();
        int cols = (int) Math.ceil(Math.sqrt(total));
        int rows = (int) Math.ceil((double) total / cols);

        System.out.println(GREEN + total + " files found. They will be shown in a " + cols + "x" + rows + " grid." + RESET);

        BufferedImage outputImage = new BufferedImage(cols * CELL_WIDTH, rows * CELL_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = outputImage.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, outputImage.getWidth(), outputImage.getHeight());

        if (ANTI_ALIASING) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }

        int processedFiles = 0;
        int skippedFiles = 0;

        for (int i = 0; i < total; i++) {
            Path svg = svgFiles.get(i);
            int col = i % cols;
            int row = i / cols;

            try {
                int xPos = col * CELL_WIDTH + PADDING;
                int yPos = row * CELL_HEIGHT + PADDING + FONT_SIZE + NUMBER_PADDING;

                int renderWidth = CELL_WIDTH - PADDING * 2 - FONT_SIZE - NUMBER_PADDING;
                int renderHeight = CELL_HEIGHT - PADDING * 2 - FONT_SIZE - NUMBER_PADDING;

                if (mode == LabelMode.NONE) {
                    yPos -= FONT_SIZE + NUMBER_PADDING;
                    renderWidth += FONT_SIZE + NUMBER_PADDING;
                    renderHeight += FONT_SIZE + NUMBER_PADDING;
                }

                BufferedImage rendered = renderSvgToImage(svg.toUri().toString(), renderWidth, renderHeight);

                int xOffset = (CELL_WIDTH - PADDING * 2 - renderWidth) / 2;
                int yOffset = -(CELL_HEIGHT - FONT_SIZE - (NUMBER_PADDING - 5) - renderHeight);

                // Отображаем текст в зависимости от режима
                if (mode != LabelMode.NONE) {
                    g2d.setColor(NUMBER_COLOR);
                    g2d.setFont(new Font(FONT_NAME, Font.BOLD, FONT_SIZE));

                    String label = "";
                    switch (mode) {
                        case NUMERATE:
                            label = String.valueOf(i + 1); // Нумерация
                            break;
                        case FILE_NAME:
                            label = svg.getFileName().toString(); // Имя файла
                            break;
                        default:
                            break;
                    }

                    g2d.drawString(label, xPos, yPos - 5);
                } else {
                    xOffset = 0;
                    yOffset = 0;
                }

                g2d.setColor(Color.WHITE);
                g2d.fillRect(xPos + xOffset, yPos + FONT_SIZE + NUMBER_PADDING, renderWidth, renderHeight);

                int imgYPos = yPos;
                if (mode != LabelMode.NONE) {
                    imgYPos += FONT_SIZE + NUMBER_PADDING;
                }
                g2d.drawImage(rendered, xPos + xOffset, imgYPos + yOffset, null);

                g2d.setColor(BORDER_COLOR);
                g2d.drawRect(col * CELL_WIDTH, row * CELL_HEIGHT, CELL_WIDTH - 1, CELL_HEIGHT - 1);

                processedFiles++;
                System.out.print(BLUE + "Processing final image (" + processedFiles + "/" + total + ")..." + RESET + "\r");
            } catch (Exception e) {
                skippedFiles++;
                System.out.println(RED + "Image " + (i + 1) + " skipped. There were issues reading this file." + RESET);
            }
        }

        g2d.dispose();
        ImageIO.write(outputImage, "png", new File("svg_grid_output.png"));

        System.out.println(GREEN + "\nDone! Check for the output file svg_grid_output.png" + RESET);
        System.out.println(YELLOW + processedFiles + " files processed successfully. " + skippedFiles + " files skipped." + RESET);
    }

    private static BufferedImage renderSvgToImage(String uri, int width, int height) throws Exception {
        PNGTranscoder transcoder = new PNGTranscoder();
        transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) width);
        transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) height);

        TranscoderInput input = new TranscoderInput(uri);
        ByteArrayOutputStream ostream = new ByteArrayOutputStream();
        TranscoderOutput output = new TranscoderOutput(ostream);

        transcoder.transcode(input, output);
        ostream.flush();

        byte[] imageData = ostream.toByteArray();
        ostream.close();

        InputStream in = new ByteArrayInputStream(imageData);
        return ImageIO.read(in);
    }
}
