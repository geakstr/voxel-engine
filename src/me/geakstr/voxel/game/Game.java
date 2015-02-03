package me.geakstr.voxel.game;

import me.geakstr.voxel.core.Window;
import me.geakstr.voxel.model.World;
import me.geakstr.voxel.render.Camera;
import me.geakstr.voxel.render.FrustumCulling;
import me.geakstr.voxel.render.Shader;
import me.geakstr.voxel.render.Transform;

public class Game {
    public static Transform transform;
    public static Shader shader;
    public static Camera camera;
    public static FrustumCulling frustum;

    public static void init() {
        shader = new Shader("simple.vs", "simple.fs").compile();

        transform = new Transform();
        camera = new Camera(100, (float) Window.width / (float) Window.height, 0.01f, 100f);
        frustum = new FrustumCulling();

        World.init(4, 16, 16, 16, 16);
        World.gen();
    }

    public static void before_render() {
        camera.input();
        camera.apply();
        frustum.update(camera.getProjectionMatrix(), camera.getViewMatrix());

        shader.bind();

        shader.set_uniform("uniform_color", 1);
        shader.set_uniform("uniform_transform", transform.getTransform());
        shader.set_uniform("uniform_camera_projection", camera.getProjectionMatrix());
        shader.set_uniform("uniform_camera_view", camera.getViewMatrix());
    }

    public static void render() {
        World.render();
    }

    public static void after_render() {
        shader.unbind();
    }
}
