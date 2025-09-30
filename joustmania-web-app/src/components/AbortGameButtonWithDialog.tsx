import * as React from 'react';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import Box from "@mui/material/Box";
import Dangerous from '@mui/icons-material/Dangerous';

interface AbortGameButtonWithDialogProps {
    disabled?: boolean,
    onConfirm: () => void
}

export default function AbortGameButtonWithDialog({
                                                      disabled = false,
                                                      onConfirm
                                                  }: AbortGameButtonWithDialogProps) {
    const [open, setOpen] = React.useState(false);

    const handleClickOpen = () => {
        setOpen(true);
    };

    const handleClose = () => {
        setOpen(false);
    };

    const onConfirmClicked = () => {
        setOpen(false);
        onConfirm()
    }


    return (
        <React.Fragment>
            <Button disabled={disabled} variant="contained" size={"large"} color={"warning"} endIcon={<Dangerous/>}
                    onClick={handleClickOpen}>
                Force Stop Game
            </Button>

            <Dialog
                open={open}
                onClose={handleClose}
                aria-labelledby="alert-dialog-title"
                aria-describedby="alert-dialog-description"
            >
                <DialogTitle id="alert-dialog-title">
                    {`Force Stop current game`}
                </DialogTitle>
                <DialogContent>
                    <DialogContentText id="alert-dialog-description">
                        Do you really want to force Stop the current game?
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Box className={"flex justify-between w-full"}>
                        <Button variant={"outlined"} className={""} onClick={handleClose}>Abort</Button>
                        <Button variant={"contained"} className={""} color={"warning"}
                                onClick={onConfirmClicked} autoFocus>
                            STOP GAME NOW
                        </Button>
                    </Box>
                </DialogActions>
            </Dialog>
        </React.Fragment>
    );
}