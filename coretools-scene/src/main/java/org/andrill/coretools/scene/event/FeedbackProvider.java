package org.andrill.coretools.scene.event;

public interface FeedbackProvider {
    Feedback getFeedback(SceneEvent e, Object target);
}