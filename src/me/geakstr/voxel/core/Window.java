package me.geakstr.voxel.core;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.glfw.GLFWvidmode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.glu.GLU;

import java.nio.ByteBuffer;

import static org.lwjgl.glfw.Callbacks.errorCallbackPrint;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_SRGB;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {
    public static int width = 800;
    public static int height = 800;
    public static boolean vsync = false;

    private static double last_time;
    private static int fps;

    private static long window;

    private static GLFWErrorCallback errorCallback;
    private static GLFWKeyCallback keyCallback;
    private static GLFWWindowSizeCallback resizeCallback;

    public static boolean was_resize = false;

    public static void init(int width, int height, boolean vsync) {
        Window.width = width;
        Window.height = height;
        Window.vsync = vsync;

        Window.last_time = glfwGetTime();
        Window.fps = 0;

        glfwSetErrorCallback(errorCallback = errorCallbackPrint(System.err));

        if (glfwInit() != GL11.GL_TRUE) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GL11.GL_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GL11.GL_TRUE);
        glfwWindowHint(GLFW_REFRESH_RATE, 100);

        window = glfwCreateWindow(Window.width, Window.height, "Hello World!", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        ByteBuffer vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (GLFWvidmode.width(vidmode) - Window.width) / 2, (GLFWvidmode.height(vidmode) - Window.height) / 2);
        glfwMakeContextCurrent(window);
        glfwSwapInterval(Window.vsync ? 1 : 0);
        glfwShowWindow(window);

        GLContext.createFromCurrent();

        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        glFrontFace(GL_CW);
        glCullFace(GL_FRONT_FACE);
        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_FRAMEBUFFER_SRGB);

        glEnable(GL_TEXTURE_2D);
        glShadeModel(GL_SMOOTH);
        glClearDepth(1.0);
        glDepthFunc(GL_LEQUAL);

        glViewport(0, 0, Math.max(width, height), Math.max(width, height));

        glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                    glfwSetWindowShouldClose(window, GL11.GL_TRUE);
                }
            }
        });

        glfwSetWindowSizeCallback(window, new GLFWWindowSizeCallback() {
            @Override
            public void invoke(long window, int width, int height) {
                if (Window.width != width || Window.height != height) {
                    Window.width = width;
                    Window.height = height;
                    Window.was_resize = true;
                }
            }
        });
    }

    public static void destroy() {
        glfwDestroyWindow(window);
        keyCallback.release();
    }

    public static void terminate() {
        glfwTerminate();
        errorCallback.release();
    }

    public static boolean should_close() {
        return glfwWindowShouldClose(window) == GL11.GL_TRUE;
    }

    public static void before_render() {
        glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
    }

    public static void after_render() {
        glfwSwapBuffers(window);
        glfwPollEvents();
    }

    public static void setup_aspect_ratio() {
        int size = Math.max(Window.width, Window.height);
        glViewport(0, 0, size, size);
    }

    public static boolean fps() {
        double current_time = glfwGetTime();
        fps++;
        if (current_time - last_time >= 1.0) {
            System.out.println(fps + " FPS");
            fps = 0;
            last_time++;
            return true;
        }
        return false;
    }
}
