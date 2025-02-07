package com.example.jetnote.ui.add_screen

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.util.Calendar
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.example.jetnote.cons.*
import com.example.jetnote.db.entities.note.Note
import com.example.jetnote.db.entities.note_and_label.NoteAndLabel
import com.example.jetnote.db.entities.note_and_todo.NoteAndTodo
import com.example.jetnote.db.entities.todo.Todo
import com.example.jetnote.fp.filterBadEmoji
import com.example.jetnote.fp.filterBadWords
import com.example.jetnote.fp.getMaterialColor
import com.example.jetnote.icons.CIRCLE_ICON_18
import com.example.jetnote.icons.DONE_ICON
import com.example.jetnote.ui.ImageDisplayed
import com.example.jetnote.ui.bottom_bar.AddEditBottomBar
import com.example.jetnote.ui.bottom_bar.RemindingNote
import com.example.jetnote.ui.media_player_screen.NoteMediaPlayer
import com.example.jetnote.ui.record_note.RecordingNote
import com.example.jetnote.vm.*
import com.google.accompanist.flowlayout.FlowRow
import java.io.File

@RequiresApi(Build.VERSION_CODES.N)
@SuppressLint(
    "UnrememberedMutableState",
    "UnusedMaterial3ScaffoldPaddingParameter"
)
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    ExperimentalComposeUiApi::class,
    ExperimentalGlideComposeApi::class
)
@Composable
fun NoteAdd(
    noteVM: NoteVM = hiltViewModel(),
    exoVM: MediaPlayerVM = hiltViewModel(),
    noteAndLabelVM: NoteAndLabelVM = hiltViewModel(),
    labelVM: LabelVM = hiltViewModel(),
    todoVM: TodoVM = hiltViewModel(),
    noteAndTodoVM: NoteAndTodoVM = hiltViewModel(),
    navController: NavController,
    uid: String
) {
    val ctx = LocalContext.current
    val internalPath = ctx.filesDir.path
    val keyboardManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = FocusRequester()
    val focusState = remember { mutableStateOf(false) }

    val observeNotesAndLabels =
        remember(noteAndLabelVM, noteAndLabelVM::getAllNotesAndLabels).collectAsState()
    val observeLabels = remember(labelVM, labelVM::getAllLabels).collectAsState()

    val observeTodoList = remember(todoVM, todoVM::getAllTodoList).collectAsState()
    val observeNoteAndTodo =
        remember(noteAndTodoVM, noteAndTodoVM::getAllNotesAndTodo).collectAsState()

    val isTitleFieldFocused = remember { mutableStateOf(false) }
    val isDescriptionFieldFocused = remember { mutableStateOf(false) }

    val titleState = rememberSaveable { mutableStateOf<String?>(null) }
        .filterBadWords()
        .filterBadEmoji()

    val descriptionState = rememberSaveable { mutableStateOf<String?>(null) }
        .filterBadWords()
//        .filterBadEmoji()

    val backgroundColor = getMaterialColor(SURFACE).toArgb()
    val backgroundColorState = rememberSaveable { mutableStateOf(backgroundColor) }
    val textColor = contentColorFor(getMaterialColor(SURFACE)).toArgb()
    val textColorState = rememberSaveable { mutableStateOf(textColor) }

    val priorityState = remember { mutableStateOf(NON) }

    val mediaFile = "$internalPath/$AUDIO_FILE/$uid.$MP3"

    //
    val dateState = mutableStateOf(Calendar.getInstance().time)

    val remindingDialogState = remember { mutableStateOf(false) }

    val recordDialogState = remember { mutableStateOf(false) }

    val imagePath = "$internalPath/$IMAGE_FILE/$uid.$JPEG"
    val bitImg = BitmapFactory.decodeFile(imagePath)
    val photoState = remember { mutableStateOf<Bitmap?>(bitImg) }
    val imageUriState = remember { mutableStateOf<Uri?>(File(imagePath).toUri()) }
//    val img by rememberSaveable { mutableStateOf(photoState) }

    val chooseImageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
            imageUriState.value = it
            noteVM::decodeBitmapImage.invoke(photoState.value, it!!, ctx).also { value -> photoState.value = value }
//            img.value = photoState.value
            noteVM::saveImageLocally.invoke(photoState.value, "$internalPath/$IMAGE_FILE", "$uid.$JPEG")
        }

    val state = rememberBottomSheetScaffoldState()

    val remindingValue = remember { mutableStateOf(0L) }

    val audioDurationState = remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        kotlin.runCatching {
            focusRequester.requestFocus()
        }
    }

    BottomSheetScaffold(
        scaffoldState = state,
        sheetPeekHeight = 50.dp,
        backgroundColor = Color(backgroundColorState.value),
        modifier = Modifier
            .navigationBarsPadding()
            .imePadding(),
        floatingActionButton = {
            Column {
                AnimatedVisibility(visible = state.bottomSheetState.isCollapsed) {
                    FloatingActionButton(
                        containerColor = getMaterialColor(OUT_LINE_VARIANT),
                        contentColor = contentColorFor(
                            backgroundColor = getMaterialColor(
                                OUT_LINE_VARIANT
                            )
                        ),
                        onClick = {

                            noteVM.addNote(
                                Note(
                                    title = titleState.value,
                                    description = descriptionState.value,
                                    priority = priorityState.value,
                                    uid = uid,
                                    reminding = remindingValue.value,
                                    date = dateState.value.toString(),
                                    audioDuration = audioDurationState.value,
                                    color = backgroundColorState.value,
                                    textColor = textColorState.value,
                                )
                            )
                            navController.navigate(HOME_ROUTE)
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = DONE_ICON),
                            null
                        )
                    }
                }
            }
        },
        sheetContent = {
            AddEditBottomBar(
                navController = navController,
                recordDialogState = recordDialogState,
                remindingDialogState = remindingDialogState,
                note = Note(uid = uid),
                backgroundColorState = backgroundColorState,
                textColorState = textColorState,
                notePriority = priorityState,
                imageLaunch = chooseImageLauncher,
                titleFieldState = titleState,
                descriptionFieldState = descriptionState,
                isTitleFieldSelected = isTitleFieldFocused,
                isDescriptionFieldSelected = isDescriptionFieldFocused,
                isCollapsed = state
            )
        }) {

        // recording dialog visibility.
        if (recordDialogState.value) {
            RecordingNote(uid = uid, dialogState = recordDialogState)
        }

        // reminding dialog visibility.
        if (remindingDialogState.value) {
            RemindingNote(
                dialogState = remindingDialogState,
                remindingValue = remindingValue,
                title = titleState.value,
                message = descriptionState.value,
                uid = uid
            )
        }

        LazyColumn(Modifier.fillMaxSize()) {

            // display the image.
            item {
                ImageDisplayed(media = imageUriState)
            }

            // display the media player.
            item {
                Spacer(modifier = Modifier.height(18.dp))
                if (
                    File(mediaFile).exists() && !recordDialogState.value
                ) {
                    NoteMediaPlayer(localMediaUid = uid)
                    audioDurationState.value =
                        exoVM.getMediaDuration(ctx, mediaFile).toInt()

                }
            }

            // The Title.
            item {

//                NoteTextField(
//                    uid = uid,
//                    gifUri = imageUriState,
//                    txtHint = "Title",
//                    txtSize = 26f,
//                    forSingleLine = true
//                )

                OutlinedTextField(
                    value = titleState.value ?: "",
                    onValueChange = { titleState.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .focusRequester(focusRequester)
                        .onFocusEvent {
                            isTitleFieldFocused.value = it.isFocused
                        },
                    placeholder = {
                        Text("Title", color = Color.Gray, fontSize = 24.sp)
                    },
                    textStyle = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.Default,
                        color = Color(textColorState.value)
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        autoCorrect = false,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            keyboardManager.moveFocus(FocusDirection.Next)
                        }
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        textColor = contentColorFor(backgroundColor = Color(backgroundColorState.value))
                    )
                )
            }

            //The Description.
            item {
//                NoteTextField(
//                    uid = uid,
//                    gifUri = imageUriState,
//                    txtHint = "Note",
//                    txtSize = 18f
//                )

                OutlinedTextField(
                    value = descriptionState.value ?: "",
                    onValueChange = { descriptionState.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusEvent {
                            isDescriptionFieldFocused.value = it.isFocused
                            focusState.value = it.isFocused
                        },
                    placeholder = {
                        Text("Note", color = Color.Gray, fontSize = 19.sp)
                    },
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.Default,
                        color = Color(textColorState.value)
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        autoCorrect = false,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Default
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            keyboardManager.clearFocus()
                        }
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            }

            // display all added labels.
            item {
                FlowRow {
                    observeLabels.value.filter {
                        observeNotesAndLabels.value.contains(
                            NoteAndLabel(uid, it.id)
                        )
                    }.forEach {
                        AssistChip(
                            onClick = { },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = CIRCLE_ICON_18),
                                    contentDescription = null,
                                    tint = Color(it.color),
                                    modifier = Modifier.size(10.dp)
                                )
                            },
                            label = {
                                it.label?.let { it1 -> Text(it1) }
                            }
                        )
                    }
                }
            }

            // display the todo list.
            item {
                observeTodoList.value.filter {
                    observeNoteAndTodo.value.contains(
                        NoteAndTodo(uid, it.id)
                    )
                }.forEach { todo ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = todo.isDone,
                            onCheckedChange = {
                                todoVM.updateTotoItem(
                                    Todo(
                                        id = todo.id,
                                        item = todo.item,
                                        isDone = !todo.isDone
                                    )
                                )
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color.Gray,
                                uncheckedColor = Color(textColorState.value)
                            )
                        )

                        todo.item?.let { item ->
                            Text(
                                text = item,
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    textDecoration = if (todo.isDone) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (todo.isDone) Color.Gray else Color(textColorState.value)
                                )
                            )
                        }
                    }
                }
            }

//            // void space.
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
            }
        }
    }
}


