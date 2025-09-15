import Box from "@mui/material/Box";
import {Table, TableBody, TableCell, TableRow, Typography} from "@mui/material";

interface ThreadTreeNodeProps {
    group: any;
    level: number;
}

export function ThreadTreeNode({group, level}: ThreadTreeNodeProps) {
    const indentStyle = {
        paddingLeft: `${level * 20}px`
    };

    return (
        <Table key={`thread-group-header-${group.name}`} sx={{width: '100%'}}>
            <TableBody>
                <TableRow sx={{width: '100%'}}>
                    <TableCell style={indentStyle}>
                        <Box sx={{display: 'flex', alignItems: 'center'}}>
                            <Typography variant="body2" sx={{fontWeight: 'bold'}}>
                                📁 {group.name}
                            </Typography>
                        </Box>
                    </TableCell>
                    <TableCell align="right">
                        <Typography variant="body2" color="text.secondary">
                            Max Priority: {group.maxPriority}, Daemon: {group.isDaemon ? 'Ja' : 'Nein'}
                            {group.parentName && `, Parent: ${group.parentName}`}
                        </Typography>
                    </TableCell>
                </TableRow>
                {/* Threads in dieser Gruppe */}
                {group.threads && group.threads.map((thread: any) => (
                    <TableRow key={`thread-${thread.id}`}>
                        <TableCell style={{paddingLeft: `${(level + 1) * 20}px`}}>
                            <Box sx={{display: 'flex', alignItems: 'center'}}>
                                <Typography variant="body2">
                                    🔵 {thread.name}
                                </Typography>
                            </Box>
                        </TableCell>
                        <TableCell align="right">
                            <Typography variant="body2" color="text.secondary">
                                ID: {thread.id}, State: {thread.state},
                                Priority: {thread.priority}, Daemon: {thread.isDaemon ? 'Ja' : 'Nein'}
                            </Typography>
                        </TableCell>
                    </TableRow>
                ))}

                {/* Untergruppen */}
                {group.subGroups && group.subGroups.map((subGroup: any, index: number) => (
                    <TableRow key={`subgroup-${subGroup.name}-${index}`}>
                        <TableCell style={{paddingLeft: `${(level + 1) * 20}px`}} colSpan={2}>
                            <ThreadTreeNode
                                key={`subgroup-${subGroup.name}-${index}`}
                                group={subGroup}
                                level={level + 1}
                            />
                        </TableCell>
                    </TableRow>
                ))}
            </TableBody>
        </Table>
    );
}
