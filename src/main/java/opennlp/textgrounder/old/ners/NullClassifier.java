///////////////////////////////////////////////////////////////////////////////
//  Copyright (C) 2010 Taesun Moon, The University of Texas at Austin
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
///////////////////////////////////////////////////////////////////////////////
package opennlp.textgrounder.old.ners;

import edu.stanford.nlp.ie.crf.*;

/**
 * An override of the CRFClassifier that doesn't really do anything. It can be
 * passed as the `classifier' argument to TextProcessor() when to actual NER
 * classification is needed. This is used, for example, in the base TopicModel.
 * 
 * The overridden `classifyToString()' method outputs its input text unmodified
 * except for lowercasing each word and removing non-alphanumeric characters.
 * 
 * FIXME: This depends on the non-obvious behavior of TextProcessor() of
 * allowing non-toponym tokens to have their '/O' tag omitted.
 * 
 * @author tsmoon
 */
public class NullClassifier extends CRFClassifier {

    /**
     * Default constructor. Does nothing.
     */
    public NullClassifier() {
    }

    /**
     * Override of sole CRFClassifier method used in TextProcessor. Merely
     * lowercases words and strips tokens of non-alphanumeric characters
     *
     * @param text string to process
     * @return input string lowercased and removed of all non-alphanumeric
     * characters
     */
    @Override
    public String classifyToString(String text) {
        StringBuffer buf = new StringBuffer();
        String[] tokens = text.split(" ");
        for (String token : tokens) {
            buf.append(token.toLowerCase().replaceAll("[^a-z0-9]", ""));
            buf.append(" ");
        }
        return buf.toString();
    }
}