package org.gwtproject.validation.context;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.validation.Constraint;

import com.google.auto.common.MoreTypes;

/**
 * @author Dmitrii Tikhomirov <chani@me.com>
 * Created by treblereel on 10/26/18.
 */
public class AptContext {

    public final Messager messager;
    public final Filer filer;
    public final Elements elements;
    public final Types types;
    public final RoundEnvironment roundEnvironment;
    public final ProcessingEnvironment processingEnv;
    final ConstraintHelper constraints = new ConstraintHelper();

    public AptContext(final ProcessingEnvironment processingEnv, final RoundEnvironment roundEnvironment) {
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        this.elements = processingEnv.getElementUtils();
        this.types = processingEnv.getTypeUtils();
        this.roundEnvironment = roundEnvironment;
        this.processingEnv = processingEnv;

        findCustomConstraint();
    }

    private void findCustomConstraint() {
        roundEnvironment.getElementsAnnotatedWith(Constraint.class)
                .stream()
                .filter(elm -> elm.getKind().equals(ElementKind.ANNOTATION_TYPE))
                .forEach(this::processCustomConstraint);
    }

    private void processCustomConstraint(Element elm) {
        Set<AnnotationMirror> annotationMirrors = new HashSet<>();
        elm.getAnnotationMirrors().forEach(ano -> processCustomConstraint(ano, annotationMirrors));
        constraints.addConstraint(elm.toString(),
                                  getMirroredTypesException(MoreTypes.asTypeElement(elm.asType())
                                                                    .getAnnotation(Constraint.class))
                                          .stream()
                                          .collect(Collectors.toList()),
                                  annotationMirrors
        );
    }

    private void processCustomConstraint(AnnotationMirror annotation, Set<AnnotationMirror> processed) {
        if (isConstraint(annotation) && !processed.contains(annotation)) {
            processed.add(annotation);
            annotation.getAnnotationType()
                    .getAnnotationMirrors()
                    .forEach(a -> processCustomConstraint(a, processed));
        }
    }

    public boolean isSupported(String annotation) {
        return constraints.getAnnotations().contains(annotation);
    }

    public ConstraintHelper.ConstraintHolder getConstraint(String annotation) {
        return constraints.get(annotation);
    }

    public List<String> getValidators(String annotation) {
        return constraints.get(annotation).getValidators();
    }

    private boolean isConstraint(AnnotationMirror elm) {
        return elm.getAnnotationType().asElement().getAnnotation(Constraint.class) != null;
    }

    private Set<String> getMirroredTypesException(Constraint constraint) {
        try {
            constraint.validatedBy();
        } catch (javax.lang.model.type.MirroredTypesException e) {
            return e.getTypeMirrors().stream().map(TypeMirror::toString).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }
}