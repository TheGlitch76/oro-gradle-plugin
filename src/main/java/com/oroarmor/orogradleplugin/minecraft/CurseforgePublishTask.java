/*
 * MIT License
 *
 * Copyright (c) 2021 OroArmor (Eli Orona)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.oroarmor.orogradleplugin.minecraft;

import java.util.ArrayList;
import java.util.Collections;

import com.matthewprenger.cursegradle.CurseArtifact;
import com.matthewprenger.cursegradle.CurseRelation;
import com.matthewprenger.cursegradle.CurseUploadTask;
import com.oroarmor.orogradleplugin.GenericExtension;
import com.oroarmor.orogradleplugin.publish.PublishProjectExtension;
import com.oroarmor.orogradleplugin.publish.PublishProjectToLocationTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;

public abstract class CurseforgePublishTask extends CurseUploadTask implements PublishProjectToLocationTask {
    @Internal
    private String releaseURL;

    @Input
    protected final Property<String> artifactName;

    public CurseforgePublishTask() {
        this.setGroup("publishProject");

        artifactName = getProject().getObjects().property(String.class);
        artifactName.convention(getProject().getExtensions().getByType(GenericExtension.class).getName() + " - " + getProject().getVersion());

        this.onlyIf(_unused -> System.getenv("CURSE_API_KEY") != null);

        this.setApiKey(System.getenv("CURSE_API_KEY"));
        CurseArtifact artifact = new CurseArtifact();

        MinecraftPublishingExtension extension = getProject().getExtensions().getByType(MinecraftPublishingExtension.class);

        artifact.setChangelog(getProject().getExtensions().getByType(PublishProjectExtension.class).getChangelog().get());

        artifact.setChangelogType("text");
        artifact.setReleaseType("release");
        artifact.setGameVersionStrings(new ArrayList<>());
        extension.getDependencies().get().forEach(artifact.getGameVersionStrings()::add);
        artifact.getGameVersionStrings().add(extension.getLoader());
        artifact.setArtifact(extension.getModTask().get());
        this.dependsOn(extension.getModTask().get());

        CurseRelation curseRelations = new CurseRelation();
        extension.getDependencies().get().forEach(curseRelations::requiredDependency);
        artifact.setCurseRelations(curseRelations);

        this.setMainArtifact(artifact);

        this.doLast(task -> {
            CurseforgePublishTask curseforgeTask = ((CurseforgePublishTask) task);
            releaseURL = "https://www.curseforge.com/minecraft/mc-mods/" + extension.getCurseforgeId() + "/files/" + curseforgeTask.getMainArtifact().getFileID();
        });
    }

    @Override
    public Object run() {
        this.getMainArtifact().setDisplayName(this.artifactName.get());
        return super.run();
    }

    @Override
    public String getReleaseURL() {
        return releaseURL;
    }

    public Property<String> getArtifactName() {
        return artifactName;
    }
}
