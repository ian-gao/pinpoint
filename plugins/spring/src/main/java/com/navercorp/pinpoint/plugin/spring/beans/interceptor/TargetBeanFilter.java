/*
 * Copyright 2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.plugin.spring.beans.interceptor;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.util.PathMatcher;
import com.navercorp.pinpoint.plugin.spring.beans.SpringBeansConfig;
import com.navercorp.pinpoint.plugin.spring.beans.SpringBeansTarget;
import com.navercorp.pinpoint.plugin.spring.beans.SpringBeansTargetScope;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;

/**
 * @author Jongho Moon <jongho.moon@navercorp.com>
 * @author jaehong.kim
 */
public class TargetBeanFilter {
    private final List<SpringBeansTarget> targets;
    private static Cache cache = new Cache();

    public static TargetBeanFilter of(ProfilerConfig profilerConfig) {
        SpringBeansConfig config = new SpringBeansConfig(profilerConfig);
        return new TargetBeanFilter(config.getTargets());
    }

    public TargetBeanFilter(Collection<SpringBeansTarget> targets) {
        this.targets = new ArrayList<SpringBeansTarget>(targets);
    }

    public void clear() {
        cache = new Cache();
    }

    public boolean isTarget(final SpringBeansTargetScope scope, final String beanName, final BeanDefinition beanDefinition) {
        if (scope == null || beanName == null || beanDefinition == null) {
            return false;
        }

        final String className = beanDefinition.getBeanClassName();
        if (className == null) {
            return false;
        }

        if (cache.contains(className)) {
            return false;
        }

        for (SpringBeansTarget target : targets) {
            // check scope.
            if (target.getScope() != scope) {
                continue;
            }

            boolean condition = false;
            // check base packages.
            if (target.getBasePackages() != null && !target.getBasePackages().isEmpty()) {
                if (!isBasePackage(target, className)) {
                    continue;
                }
                condition = true;
            }

            // check bean name pattern.
            if (target.getNamePatterns() != null && !target.getNamePatterns().isEmpty()) {
                if (!isBeanNameTarget(target, beanName)) {
                    continue;
                }
                condition = true;
            }

            // check class name pattern.
            if (target.getClassPatterns() != null && !target.getClassPatterns().isEmpty()) {
                if (!isClassNameTarget(target, className)) {
                    continue;
                }
                condition = true;
            }

            // check class annotation.
            if (target.getAnnotations() != null && !target.getAnnotations().isEmpty()) {
                if (!(beanDefinition instanceof AnnotatedBeanDefinition) || !isAnnotationTarget(target, (AnnotatedBeanDefinition) beanDefinition)) {
                    continue;
                }
                condition = true;
            }

            if (condition) {
                // AND condition.
                return true;
            }
        }

        return false;
    }

    public boolean isTarget(final SpringBeansTargetScope scope, final String beanName, final Class<?> clazz) {
        if (scope == null || beanName == null || clazz == null) {
            return false;
        }

        final String className = clazz.getName();
        if (className == null) {
            return false;
        }

        if (cache.contains(className)) {
            return false;
        }

        for (SpringBeansTarget target : targets) {
            // check scope.
            if (target.getScope() != scope) {
                continue;
            }

            boolean condition = false;
            // check base packages.
            if (target.getBasePackages() != null && !target.getBasePackages().isEmpty()) {
                if (!isBasePackage(target, className)) {
                    continue;
                }
                condition = true;
            }

            // check bean name pattern.
            if (target.getNamePatterns() != null && !target.getNamePatterns().isEmpty()) {
                if (!isBeanNameTarget(target, beanName)) {
                    continue;
                }
                condition = true;
            }

            // check class name pattern.
            if (target.getClassPatterns() != null && !target.getClassPatterns().isEmpty()) {
                if (!isClassNameTarget(target, className)) {
                    continue;
                }
                condition = true;
            }

            // check class annotation.
            if (target.getAnnotations() != null && !target.getAnnotations().isEmpty()) {
                if (!isAnnotationTarget(target, clazz)) {
                    continue;
                }
                condition = true;
            }

            if (condition) {
                // AND condition.
                return true;
            }
        }

        return false;
    }

    private boolean isBasePackage(final SpringBeansTarget target, final String className) {
        for (String basePackage : target.getBasePackages()) {
            if (className.startsWith(basePackage)) {
                return true;
            }
        }

        return false;
    }

    private boolean isBeanNameTarget(final SpringBeansTarget target, final String beanName) {
        for (PathMatcher pathMatcher : target.getNamePatterns()) {
            if (pathMatcher.isMatched(beanName)) {
                return true;
            }
        }

        return false;
    }

    private boolean isClassNameTarget(final SpringBeansTarget target, final String className) {
        for (PathMatcher pathMatcher : target.getClassPatterns()) {
            if (pathMatcher.isMatched(className)) {
                return true;
            }
        }

        return false;
    }

    private boolean isAnnotationTarget(final SpringBeansTarget target, final AnnotatedBeanDefinition annotatedBeanDefinition) {
        for (String annotationName : target.getAnnotations()) {
            if (annotatedBeanDefinition.getMetadata().hasAnnotation(annotationName)) {
                // annotation.
                return true;
            }

            if (annotatedBeanDefinition.getMetadata().hasMetaAnnotation(annotationName)) {
                // meta annotation.
                return true;
            }
        }

        return false;
    }

    private boolean isAnnotationTarget(final SpringBeansTarget target, final Class<?> clazz) {
        for (Annotation a : clazz.getAnnotations()) {
            if (target.getAnnotations().contains(a.annotationType().getName())) {
                return true;
            }
        }

        for (Annotation a : clazz.getAnnotations()) {
            for (Annotation ac : a.annotationType().getAnnotations()) {
                if (target.getAnnotations().contains(ac.annotationType().getName())) {
                    return true;
                }
            }
        }

        return false;
    }

    public void addTransformed(final String className) {
        cache.put(className);
    }
}