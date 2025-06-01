import * as React from 'react';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import PlayCircleOutline from '@mui/icons-material/PlayCircleOutline';
import Box from "@mui/material/Box";


interface ConfirmGameStartButtonWithDialogProps {
    gameNameToStart?: string;
    gameMode: string;
    onConfirm: (gameMode: string) => void
}

export default function ConfirmGameStartButtonWithDialog({
                                                             gameNameToStart,
                                                             gameMode,
                                                             onConfirm
                                                         }: ConfirmGameStartButtonWithDialogProps) {
    const [open, setOpen] = React.useState(false);

    const handleClickOpen = () => {
        setOpen(true);
    };

    const handleClose = () => {
        setOpen(false);
    };

    const onConfirmClicked = () => {
        setOpen(false);
        onConfirm(gameMode)
    }


    return (
        <React.Fragment>
            <Button variant="contained" size={"large"} endIcon={<PlayCircleOutline/>} onClick={handleClickOpen}>
                Force Start Game
            </Button>

            <Dialog
                open={open}
                onClose={handleClose}
                aria-labelledby="alert-dialog-title"
                aria-describedby="alert-dialog-description"
            >
                <DialogTitle id="alert-dialog-title">
                    {`Force Start '${gameNameToStart}'`}
                </DialogTitle>
                <DialogContent>
                    <DialogContentText id="alert-dialog-description">
                        Do you really want to force Start the game?
                    </DialogContentText>
                </DialogContent>
                <DialogActions >
                    <Box className={"flex justify-between w-full"}>
                        <Button variant={"outlined"} className={""} onClick={handleClose}>Abort</Button>
                        <Button variant={"contained"} className={""} color={"primary"}
                                onClick={onConfirmClicked} autoFocus>
                            Start Game
                        </Button>
                    </Box>
                </DialogActions>
            </Dialog>
        </React.Fragment>
    );
}