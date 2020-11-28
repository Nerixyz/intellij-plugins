/*
 * Copyright (C) 2020 ThoughtWorks, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.thoughtworks.gauge.lexer;

import com.intellij.lexer.*;
import com.intellij.psi.tree.IElementType;
import static com.thoughtworks.gauge.language.token.SpecTokenTypes.*;

%%

%{
  public _SpecLexer() {
    this(null);
  }
%}

%public
%class _SpecLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode
%caseless
%state INSTEP,INARG,INDYNAMIC,INTABLEHEADER,INTABLEBODY,INTABLEBODYROW,INDYNAMICTABLEITEM,INTABLECELL,INCOMMENT

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
WhiteSpace = [ \t\f]
TableIdentifier = [|]
InputCharacter = [^\r\n]
StepIdentifier = [*]
NonWhiteSpaceAndIdentifierCharacter = [^ \r\n\t\f#*|]
dynamicArgTablePattern = {WhiteSpace}* [<] {tableChar}* [>] {WhiteSpace}*
ScenarioHeading = {WhiteSpace}* "##" {InputCharacter}* {LineTerminator}? | {WhiteSpace}* {InputCharacter}* {LineTerminator} [-]+ {LineTerminator}?
SpecHeading = {WhiteSpace}* "#" {InputCharacter}* {LineTerminator}? | {WhiteSpace}* {InputCharacter}* {LineTerminator} [=]+ {LineTerminator}?
Tags = {WhiteSpace}* tags {WhiteSpace}? ":" {InputCharacter}* {LineTerminator}?
Keyword = {WhiteSpace}* table {WhiteSpace}? ":" {InputCharacter}* {LineTerminator}?
TeardownIdentifier = "_" "_" "_"+ {WhiteSpace}* {LineTerminator}
%%
<YYINITIAL> {
  {ScenarioHeading}                        {return SCENARIO_HEADING;}
  {SpecHeading}                            {return SPEC_HEADING;}
  {Tags}                                   {return TAGS;}
  {Keyword}                                {return KEYWORD;}
  {StepIdentifier}                         {yybegin(INSTEP); return STEP_IDENTIFIER;}
  {TableIdentifier}                        {yybegin(INTABLEHEADER); return TABLE_BORDER;}
  {LineTerminator}? {WhiteSpace}* {NonWhiteSpaceAndIdentifierCharacter}+ {WhiteSpace}* ({StepIdentifier} | [#] | [##] | {TableIdentifier}) {InputCharacter}* {LineTerminator}? {return COMMENT;}
  {TeardownIdentifier}                     {return TEARDOWN_IDENTIFIER;}
  {LineTerminator}?{WhiteSpace}*           {return COMMENT;}
  [^]                                      {yypushback(1); yybegin(INCOMMENT);}
}

<INSTEP> {
  [^<\"\r\n]*                              {yybegin(INSTEP); return STEP;}
  [\"]                                     {yybegin(INARG); return ARG_START;}
  [<]                                      {yybegin(INDYNAMIC); return DYNAMIC_ARG_START;}
  {LineTerminator}?                        {yybegin(YYINITIAL); return STEP;}
}

<INARG> {
  (\\\"|[^\"])*                            {return ARG;}
  [\"]                                     {yybegin(INSTEP); return ARG_END;}
}

<INDYNAMIC> {
  (\\<|\\>|[^\>])*                         {return DYNAMIC_ARG;}
  [>]                                      {yybegin(INSTEP); return DYNAMIC_ARG_END;}
}

<INDYNAMICTABLEITEM> {
  (\\<|\\>|[^\>|]|[\\\|])                  {yybegin(INDYNAMICTABLEITEM); return DYNAMIC_ARG; }
  [>]                                      {yybegin(INTABLEBODYROW); return DYNAMIC_ARG_END;}
  {TableIdentifier}                        {yybegin(INTABLEBODYROW); return TABLE_BORDER;}
  {LineTerminator}{WhiteSpace}*            {yybegin(INTABLEBODY); return NEW_LINE;}
}

<INTABLEHEADER> {
  (\\\||[^|\r\n])*                         {yybegin(INTABLEHEADER); return TABLE_HEADER;}
  {TableIdentifier}                        {yybegin(INTABLEHEADER); return TABLE_BORDER;}
  {LineTerminator}{WhiteSpace}*            {yybegin(INTABLEBODY); return NEW_LINE;}
}

<INTABLEBODY> {
  {TableIdentifier}                        {yybegin(INTABLEBODYROW); return TABLE_BORDER;}
  [^]                                      {yypushback(1); yybegin(YYINITIAL);}
}

<INTABLEBODYROW> {
  {WhiteSpace}*                            {yybegin(INTABLEBODYROW); return WHITESPACE;}
  (\\\||[^-<|\r\n])                        {yybegin(INTABLECELL); return TABLE_ROW_VALUE;}
  [-]*                                     {yybegin(INTABLEBODYROW); return TABLE_BORDER;}
  [<]                                      {yybegin(INDYNAMICTABLEITEM); return DYNAMIC_ARG_START;}
  {TableIdentifier}                        {yybegin(INTABLEBODYROW); return TABLE_BORDER;}
  {LineTerminator}{WhiteSpace}*            {yybegin(INTABLEBODY); return NEW_LINE;}
}

<INTABLECELL> {
  (\\\||[^|\r\n])*                         {yybegin(INTABLECELL); return TABLE_ROW_VALUE;}
  {TableIdentifier}                        {yybegin(INTABLEBODYROW); return TABLE_BORDER;}
  {LineTerminator}{WhiteSpace}*            {yybegin(INTABLEBODY); return NEW_LINE;}
}

<INCOMMENT> {
  {InputCharacter}+{LineTerminator}?       {yybegin(YYINITIAL); return COMMENT;}
  {LineTerminator}?                        {yybegin(YYINITIAL); return COMMENT;}
}