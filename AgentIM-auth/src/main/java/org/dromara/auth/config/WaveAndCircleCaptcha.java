package org.dromara.auth.config;

import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.img.GraphicsUtil;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 带干扰线、波浪、圆的验证码
 *
 * @author Lion Li
 */
public class WaveAndCircleCaptcha extends AbstractCaptcha {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 创建默认长度的波浪圆形干扰验证码。
     *
     * <p>该构造器使用 4 位随机字符和默认干扰数量，适用于 Auth 模块未显式指定验证码长度的
     * 常规登录、注册验证码场景。</p>
     *
     * @param width 验证码图片宽度，单位像素
     * @param height 验证码图片高度，单位像素
     */
    public WaveAndCircleCaptcha(int width, int height) {
        this(width, height, 4);
    }

    /**
     * 创建指定字符数量的波浪圆形干扰验证码。
     *
     * <p>该构造器允许调用方控制验证码字符数量，同时沿用默认干扰强度。字符生成器使用
     * Hutool 的 {@link RandomGenerator}，保证与框架原有验证码行为兼容。</p>
     *
     * @param width 验证码图片宽度，单位像素
     * @param height 验证码图片高度，单位像素
     * @param codeCount 验证码字符数量
     */
    public WaveAndCircleCaptcha(int width, int height, int codeCount) {
        this(width, height, codeCount, 6);
    }

    /**
     * 创建指定字符数量和干扰数量的波浪圆形干扰验证码。
     *
     * <p>该构造器用于需要调节验证码识别难度的场景：字符数量决定用户输入长度，干扰数量决定
     * 圆形噪点和波浪线的组合强度。</p>
     *
     * @param width 验证码图片宽度，单位像素
     * @param height 验证码图片高度，单位像素
     * @param codeCount 验证码字符数量
     * @param interfereCount 干扰元素数量，至少为 1 时会绘制波浪线
     */
    public WaveAndCircleCaptcha(int width, int height, int codeCount, int interfereCount) {
        this(width, height, new RandomGenerator(codeCount), interfereCount);
    }

    /**
     * 使用自定义字符生成器创建验证码。
     *
     * <p>该构造器保留 Hutool 验证码扩展点，调用方可以传入数字、字母或业务自定义字符生成器；
     * 当前类只负责图片绘制、抗锯齿、扭曲和干扰元素，不改变字符生成策略。</p>
     *
     * @param width 验证码图片宽度，单位像素
     * @param height 验证码图片高度，单位像素
     * @param generator 验证码字符生成器
     * @param interfereCount 干扰元素数量
     */
    public WaveAndCircleCaptcha(int width, int height, CodeGenerator generator, int interfereCount) {
        super(width, height, generator, interfereCount);
    }

    /**
     * 创建指定字体大小的波浪圆形干扰验证码。
     *
     * <p>该构造器在字符数量和干扰数量之外暴露字体大小配置，适合不同前端展示尺寸下保持字符
     * 可读性。P0 暂不实现前端，但 Auth 接口仍可按配置生成适合客户端展示的图片。</p>
     *
     * @param width 验证码图片宽度，单位像素
     * @param height 验证码图片高度，单位像素
     * @param codeCount 验证码字符数量
     * @param interfereCount 干扰元素数量
     * @param size 字体大小
     */
    public WaveAndCircleCaptcha(int width, int height, int codeCount, int interfereCount, float size) {
        super(width, height, new RandomGenerator(codeCount), interfereCount, size);
    }

    /**
     * 根据验证码文本生成图片。
     *
     * <p>方法创建透明或 RGB 背景的缓冲图片，依次绘制彩色字符、执行 X/Y 扭曲并叠加圆形与波浪线
     * 干扰。所有绘制操作都在本方法内完成，最后释放 {@link Graphics2D} 资源，避免验证码请求
     * 频繁时出现图形上下文泄漏。</p>
     *
     * @param code 需要渲染到图片上的验证码文本
     * @return 渲染完成的验证码图片
     */
    @Override
    public Image createImage(String code) {
        final BufferedImage image = new BufferedImage(
            width,
            height,
            (null == this.background) ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_INT_RGB
        );
        final Graphics2D g = ImgUtil.createGraphics(image, this.background);

        try {
            drawString(g, code);
            // 扭曲
            shear(g, this.width, this.height, ObjectUtil.defaultIfNull(this.background, Color.WHITE));
            drawInterfere(g);
        } finally {
            g.dispose();
        }

        return image;
    }

    /**
     * 绘制验证码字符。
     *
     * <p>该方法开启图形和文字抗锯齿，并在配置了透明度时应用 textAlpha，然后交由 Hutool
     * 按彩色字符策略绘制文本。字符绘制与干扰绘制分离，方便后续调整干扰强度而不影响可读性。</p>
     *
     * @param g 当前验证码图片的二维绘图上下文
     * @param code 验证码文本
     */
    private void drawString(Graphics2D g, String code) {
        // 设置抗锯齿（让字体渲染更清晰）
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (this.textAlpha != null) {
            g.setComposite(this.textAlpha);
        }

        GraphicsUtil.drawStringColourful(g, code, this.font, this.width, this.height);
    }

    /**
     * 绘制验证码干扰元素。
     *
     * <p>干扰元素由若干随机圆形和一条平滑波浪线组成。圆形用于增加背景噪声，波浪线用于增加
     * 机器识别难度；当 interfereCount 小于 1 时只保留字符和扭曲效果。</p>
     *
     * @param g 当前验证码图片的二维绘图上下文
     */
    protected void drawInterfere(Graphics2D g) {
        ThreadLocalRandom random = RandomUtil.getRandom();
        int circleCount = Math.max(0, this.interfereCount - 1);

        // 圈圈
        for (int i = 0; i < circleCount; i++) {
            g.setColor(ImgUtil.randomColor(random));
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            int w = random.nextInt(height >> 1);
            int h = random.nextInt(height >> 1);
            g.drawOval(x, y, w, h);
        }

        // 仅 1 条平滑波浪线
        if (this.interfereCount >= 1) {
            g.setColor(getRandomColor(120, 230, random));
            drawSmoothWave(g, random);
        }
    }

    /**
     * 绘制单条平滑波浪干扰线。
     *
     * <p>方法随机生成振幅、波长和相位，并把基线限制在图片中部区域，避免波浪线完全贴边导致
     * 干扰失效。计算出的点位会被裁剪到图片边界内，再使用折线一次性绘制。</p>
     *
     * @param g 当前验证码图片的二维绘图上下文
     * @param random 当前线程随机数生成器
     */
    private void drawSmoothWave(Graphics2D g, ThreadLocalRandom random) {
        int amplitude = random.nextInt(8) + 5;        // 波动幅度
        int wavelength = random.nextInt(40) + 30;     // 波长
        double phase = random.nextDouble() * Math.PI * 2;

        // ✅ 关键：限制 baseY 在中间区域
        int centerY = height / 2;
        int verticalJitter = Math.max(5, height / 6); // 至少偏移5像素
        int baseY = centerY - verticalJitter + random.nextInt(verticalJitter * 2);

        g.setStroke(new BasicStroke(2.5f)); // 线宽

        int[] xPoints = new int[width];
        int[] yPoints = new int[width];
        for (int x = 0; x < width; x++) {
            int y = baseY + (int) (amplitude * Math.sin((double) x / wavelength * 2 * Math.PI + phase));
            // 限制 y 不要超出图像边界（可选）
            y = Math.max(amplitude, Math.min(y, height - amplitude));
            xPoints[x] = x;
            yPoints[x] = y;
        }
        g.drawPolyline(xPoints, yPoints, width);
    }

    /**
     * 生成指定亮度区间内的随机灰阶颜色。
     *
     * <p>波浪线使用中高亮度颜色，既能形成干扰，又避免覆盖验证码字符导致用户难以识别。RGB
     * 三个通道使用同一区间随机值，整体呈现相对柔和的灰阶色。</p>
     *
     * @param min 单个 RGB 通道的最小值
     * @param max 单个 RGB 通道的最大值
     * @param random 当前线程随机数生成器
     * @return 随机生成的颜色
     */
    private Color getRandomColor(int min, int max, ThreadLocalRandom random) {
        int range = max - min;
        return new Color(
            min + random.nextInt(range),
            min + random.nextInt(range),
            min + random.nextInt(range)
        );
    }

    /**
     * 扭曲
     *
     * @param g     {@link Graphics}
     * @param w1    w1
     * @param h1    h1
     * @param color 颜色
     */
    private void shear(Graphics g, int w1, int h1, Color color) {
        shearX(g, w1, h1, color);
        shearY(g, w1, h1, color);
    }

    /**
     * X坐标扭曲
     *
     * @param g     {@link Graphics}
     * @param w1    宽
     * @param h1    高
     * @param color 颜色
     */
    private void shearX(Graphics g, int w1, int h1, Color color) {

        int period = RandomUtil.randomInt(this.width);

        int frames = 1;
        int phase = RandomUtil.randomInt(2);

        for (int i = 0; i < h1; i++) {
            double d = (double) (period >> 1) * Math.sin((double) i / (double) period + (6.2831853071795862D * (double) phase) / (double) frames);
            g.copyArea(0, i, w1, 1, (int) d, 0);
            g.setColor(color);
            g.drawLine((int) d, i, 0, i);
            g.drawLine((int) d + w1, i, w1, i);
        }

    }

    /**
     * Y坐标扭曲
     *
     * @param g     {@link Graphics}
     * @param w1    宽
     * @param h1    高
     * @param color 颜色
     */
    private void shearY(Graphics g, int w1, int h1, Color color) {

        int period = RandomUtil.randomInt(this.height >> 1);

        int frames = 20;
        int phase = 7;
        for (int i = 0; i < w1; i++) {
            double d = (double) (period >> 1) * Math.sin((double) i / (double) period + (6.2831853071795862D * (double) phase) / (double) frames);
            g.copyArea(i, 0, 1, h1, 0, (int) d);
            g.setColor(color);
            // 擦除原位置的痕迹
            g.drawLine(i, (int) d, i, 0);
            g.drawLine(i, (int) d + h1, i, h1);
        }

    }

}
